package io.github.pm665.telegramboot.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = BackendConfigProperties.BACKEND_CONFIG_PROPERTY_PREFIX)
data class BackendConfigProperties(
    val recurringJobs: Map<String, RecurringJobProperties> = emptyMap(),
    val commands: Map<String, CommandProperties> = emptyMap(),
    val creatorId: Long = 0,
) {
    data class RecurringJobProperties(
        val order: Long = 0,
        val enabled: Boolean,
        val interval: Duration,
    )

    data class CommandProperties(
        val enabled: Boolean,
    )

    companion object {
        const val BACKEND_CONFIG_PROPERTY_PREFIX: String = "backend"
    }
}
