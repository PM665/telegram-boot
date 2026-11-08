package io.github.pm665.telegramboot.observability

import io.github.pm665.telegramboot.domain.CommandAndState
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotInstance
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import io.github.pm665.telegramboot.drivers.bot.BotActionExecutorService
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.drivers.scheduling.AbstractBotRecurringJob
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.BotUpdateObserver
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import java.util.concurrent.atomic.AtomicReference

class TracingEntryPointsPhase2Test {
    @Test
    fun `telegram update root observation wraps update observer and command execution`() {
        val observationRegistry = ObservationRegistry.create()
        val scopeTracker = ObservationScopeTracker()
        observationRegistry.observationConfig().observationHandler(scopeTracker)
        val chatStatesService = mock<ChatStatesService>()
        val botActionExecutorService = mock<BotActionExecutorService>()
        val callbackDataService = mock<CallbackDataService>()
        val applicationEventPublisher = mock<ApplicationEventPublisher>()
        val observerObservationName = AtomicReference<String?>()
        val executorObservationName = AtomicReference<String?>()
        val update = messageUpdate("/start", "tg_alice")
        val observer =
            BotUpdateObserver { _, _ ->
                observerObservationName.set(scopeTracker.currentObservationName())
                false
            }

        whenever(chatStatesService.getChatState(42L, "espresso_bot")).thenReturn(null)
        doAnswer {
            executorObservationName.set(scopeTracker.currentObservationName())
            null
        }.whenever(botActionExecutorService).executeActionForCommandAndState(any<CommandAndState>(), eq("espresso_bot"), eq(update))

        val handler =
            BotUpdateHandler(
                chatStatesService,
                botActionExecutorService,
                callbackDataService,
                applicationEventPublisher,
                listOf(observer),
                observationRegistry,
            )

        handler.handleUpdate("espresso_bot", update)

        assertThat(observerObservationName.get()).isEqualTo("bot.update")
        assertThat(executorObservationName.get()).isEqualTo("bot.update")
    }

    @Test
    fun `custom recurring job starts root observation at trigger boundary`() {
        val observationRegistry = ObservationRegistry.create()
        val scopeTracker = ObservationScopeTracker()
        observationRegistry.observationConfig().observationHandler(scopeTracker)
        val currentObservationName = AtomicReference<String?>()
        val bot =
            mock<BotInstance> {
                on { isRunning } doReturn true
                on { botUsername } doReturn "espresso_bot"
            }
        val applicationContext =
            mock<ApplicationContext> {
                on { getBeansOfType(BotInstance::class.java) } doReturn mapOf("espresso_bot" to bot)
            }
        val observationRegistryProvider =
            mock<ObjectProvider<ObservationRegistry>> {
                on { ifAvailable } doReturn observationRegistry
            }
        val job =
            object : AbstractBotRecurringJob() {
                override fun execute(bot: BotInstance) {
                    currentObservationName.set(scopeTracker.currentObservationName())
                }

                override fun jobId(): String = "espresso-job"
            }

        job.applicationContext = applicationContext
        job.observationRegistryProvider = observationRegistryProvider
        job.executeJob()

        assertThat(currentObservationName.get()).isEqualTo("telegram.recurring-job")
    }

    private fun messageUpdate(
        text: String,
        username: String,
    ): BotUpdate =
        BotUpdate(
            message =
                BotIncomingMessage(
                    text = text,
                    chat = BotChatInfo(42L, BotChatType.PRIVATE),
                    from = BotUserInfo(42L, username, "en", "Alice"),
                ),
        )

    private class ObservationScopeTracker : ObservationHandler<Observation.Context> {
        private val currentObservationName = ThreadLocal<String?>()

        override fun supportsContext(context: Observation.Context): Boolean = true

        override fun onScopeOpened(context: Observation.Context) {
            currentObservationName.set(context.name)
        }

        override fun onScopeClosed(context: Observation.Context) {
            currentObservationName.remove()
        }

        fun currentObservationName(): String? = currentObservationName.get()
    }
}
