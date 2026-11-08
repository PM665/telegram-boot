package io.github.pm665.telegramboot.drivers.scheduling

import io.github.pm665.telegramboot.domain.bot.BotInstance
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

abstract class AbstractBotRecurringJob {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var observationRegistryProvider: ObjectProvider<ObservationRegistry>

    open fun executeJob() {
        Observation.createNotStarted("telegram.recurring-job", observationRegistryProvider.ifAvailable ?: ObservationRegistry.NOOP)
            .lowCardinalityKeyValue("job_id", jobId())
            .contextualName("telegram recurring job")
            .observe {
                MDC.putCloseable("bot_job", jobId()).use {
                    execute(getRunningBots())
                }
            }
    }

    protected open fun execute(bots: Collection<BotInstance>) {
        bots.forEach(::executeWithBot)
    }

    private fun getRunningBots(): Collection<BotInstance> =
        applicationContext
            .getBeansOfType(BotInstance::class.java)
            .values
            .filter(BotInstance::isRunning)

    private fun executeWithBot(bot: BotInstance) {
        MDC.putCloseable("bot", bot.botUsername).use {
            try {
                execute(bot)
            } catch (e: RuntimeException) {
                log.error(
                    "Unexpected error occurred during the execution of the job: {} {}",
                    e.javaClass,
                    e.message,
                    e,
                )
            }
        }
    }

    protected abstract fun execute(bot: BotInstance)

    abstract fun jobId(): String

    companion object {
        private val log = LoggerFactory.getLogger(AbstractBotRecurringJob::class.java)
    }
}
