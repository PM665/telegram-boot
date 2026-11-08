package io.github.pm665.telegramboot.drivers.scheduling

import io.github.pm665.telegramboot.infrastructure.BackendConfigProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.ZonedDateTime

@Configuration
@EnableScheduling
class RecurringJobsConfiguration {
    @Autowired
    fun initRecurringJobs(
        taskScheduler: TaskScheduler,
        backendConfigProperties: BackendConfigProperties,
        jobs: List<AbstractBotRecurringJob>?,
    ) {
        jobs.orEmpty().forEach { job ->
            val id = job.jobId()
            val config = backendConfigProperties.recurringJobs[id]
            if (config != null && config.enabled) {
                val taskDistributionDelay = 60L * config.order + (60L * Math.random()).toLong()
                taskScheduler.scheduleWithFixedDelay(
                    job::executeJob,
                    ZonedDateTime.now().plusSeconds(taskDistributionDelay).toInstant(),
                    config.interval,
                )
            }
        }
    }
}
