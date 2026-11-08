package io.github.pm665.telegramboot.core.autoconfigure

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.features.callbacks.CallbackData
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.features.users.InMemoryUsersService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.UsersService
import io.github.pm665.telegramboot.ports.UsersService.Chat
import io.github.pm665.telegramboot.ports.UsersService.User
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import java.util.concurrent.ScheduledThreadPoolExecutor

class TelegramBootCoreAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withPropertyValues("spring.main.lazy-initialization=true")
            .withBean(MessageSender::class.java, { mock(MessageSender::class.java) })
            .withBean(CallbackDataService::class.java, { TestCallbackDataService() })
            .withBean(BotsService::class.java, { TestBotsService() })
            .withBean(ObservationRegistry::class.java, { ObservationRegistry.create() })
            .withBean(TaskScheduler::class.java, { ConcurrentTaskScheduler(ScheduledThreadPoolExecutor(10)) })
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withBean(ChatStatesService::class.java, { TestChatStatesService() })
            .withConfiguration(AutoConfigurations.of(TelegramBootCoreAutoConfiguration::class.java))

    @Test
    fun `imports telegram boot core configuration`() {
        contextRunner.run { context ->
            val usersServices = context.getBeansOfType(UsersService::class.java)
            assertThat(usersServices).hasSize(1)
            assertThat(usersServices.values.single()).isInstanceOf(InMemoryUsersService::class.java)

            assertThat(context.getBeansOfType(io.github.pm665.telegramboot.domain.DefaultBotActionContextProvider::class.java))
                .hasSize(1)
        }
    }

    @Test
    fun `backs off when telegram boot core module missing`() {
        ApplicationContextRunner()
            .withClassLoader(FilteredClassLoader("io.github.pm665.telegramboot.TelegramBootConfiguration"))
            .withConfiguration(AutoConfigurations.of(TelegramBootCoreAutoConfiguration::class.java))
            .run { context ->
                assertThat(context.getBeansOfType(UsersService::class.java)).isEmpty()
            }
    }

    @Test
    fun `backs off when custom users service provided`() {
        val customService = CustomUsersService()

        contextRunner
            .withBean(UsersService::class.java, { customService })
            .run { context ->
                val usersServices = context.getBeansOfType(UsersService::class.java)
                assertThat(usersServices).hasSize(1)
                assertThat(usersServices.values.single()).isSameAs(customService)

                assertThat(
                    context.getBeansOfType(io.github.pm665.telegramboot.domain.DefaultBotActionContextProvider::class.java),
                ).hasSize(1)
            }
    }

    private class CustomUsersService : UsersService {
        override fun getUser(
            botUsername: String,
            userId: Long,
        ): User? = null

        override fun getUsers(
            botUsername: String,
            userIds: Collection<Long>,
        ): Collection<User> = emptyList()

        override fun findUserByUsername(
            botUsername: String,
            username: String,
        ): User? = null

        override fun getChat(
            botUsername: String,
            chatId: Long,
        ): Chat? = null

        override fun getChats(
            botUsername: String,
            userIds: Collection<Long>,
        ): Collection<Chat> = emptyList()

        override fun getChatsByBotUserIds(
            botUsername: String,
            botUserIds: Collection<Long>,
        ): Collection<Chat> = emptyList()

        override fun getChats(
            botUsername: String,
            accessType: AccessType,
        ): Collection<Chat> = emptyList()

        override fun createNewUser(
            botUsername: String,
            userId: Long,
            username: String?,
            locale: String,
        ): User = User(userId, username, locale, botUsername = botUsername)

        override fun createNewChat(
            userId: Long,
            botUsername: String,
            chatId: Long,
        ): Chat = Chat(userId, botUsername, chatId, true)

        override fun setChatEnabled(
            botUsername: String,
            chatId: Long,
            enabled: Boolean,
        ) = Unit
    }

    private class TestCallbackDataService : CallbackDataService {
        override fun findById(id: String): CallbackData? = null

        override fun createNew(callbackData: CallbackData) = Unit
    }

    private class TestBotsService : BotsService {
        override fun findAllActiveBots(): Collection<BotsService.Bot> = emptyList()

        override fun findBotByBotUsername(botUsername: String): BotsService.Bot? = null
    }

    private class TestChatStatesService : ChatStatesService {
        override fun getChatState(
            chatId: Long,
            botUsername: String,
        ): ChatStatesService.ChatState? = null

        override fun setChatState(state: ChatStatesService.ChatState) = Unit

        override fun deleteChatState(
            chatId: Long,
            botUsername: String,
        ) = Unit
    }
}
