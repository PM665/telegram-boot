package io.github.pm665.telegramboot.observability

import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.micrometer.context.ContextRegistry
import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.support.ContextPropagatingTaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class TracingAsyncPropagationPhase3Test {
    companion object {
        init {
            ContextRegistry.getInstance()
                .registerThreadLocalAccessor(
                    Slf4jThreadLocalAccessor("bot", "botUsername", "chat", "chatId", "command", "callback", "tgUsername"),
                )
        }
    }

    @Test
    fun `immediate async bot work preserves active observation across thread hop`() {
        val observationRegistry = ObservationRegistry.create()
        val scopeTracker = ObservationScopeTracker()
        observationRegistry.observationConfig().observationHandler(scopeTracker)
        val currentObservationName = AtomicReference<String?>()
        val currentChatId = AtomicReference<String?>()
        val taskExecutorService = TaskExecutorService(taskScheduler(), taskExecutor())
        val latch = CountDownLatch(1)

        Observation.createNotStarted("telegram.update", observationRegistry).observe {
            MDC.put("chatId", "42")
            taskExecutorService.executeImmediately {
                currentObservationName.set(scopeTracker.currentObservationName())
                currentChatId.set(MDC.get("chatId"))
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(currentObservationName.get()).isEqualTo("telegram.update")
        assertThat(currentChatId.get()).isEqualTo("42")
    }

    @Test
    fun `delayed bot work preserves active observation when scheduled task later runs`() {
        val observationRegistry = ObservationRegistry.create()
        val scopeTracker = ObservationScopeTracker()
        observationRegistry.observationConfig().observationHandler(scopeTracker)
        val currentObservationName = AtomicReference<String?>()
        val currentChatId = AtomicReference<String?>()
        val taskExecutorService = TaskExecutorService(taskScheduler(), taskExecutor())
        val latch = CountDownLatch(1)

        Observation.createNotStarted("telegram.update", observationRegistry).observe {
            MDC.put("chatId", "42")
            taskExecutorService.executeDelayed(
                {
                    currentObservationName.set(scopeTracker.currentObservationName())
                    currentChatId.set(MDC.get("chatId"))
                    latch.countDown()
                },
                Duration.ofMillis(25),
            )
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(currentObservationName.get()).isEqualTo("telegram.update")
        assertThat(currentChatId.get()).isEqualTo("42")
    }

    private fun taskExecutor(): SimpleAsyncTaskExecutor =
        SimpleAsyncTaskExecutor("telegram-boot-async-").apply {
            setVirtualThreads(true)
            setTaskDecorator(ContextPropagatingTaskDecorator())
        }

    private fun taskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("telegram-boot-test-scheduler-")
            initialize()
        }

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
