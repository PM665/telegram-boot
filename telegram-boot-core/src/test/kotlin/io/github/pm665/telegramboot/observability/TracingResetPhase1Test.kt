package io.github.pm665.telegramboot.observability

import io.github.pm665.telegramboot.TelegramBootConfiguration
import io.github.pm665.telegramboot.domain.CommandAndState
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.drivers.bot.BotActionExecutorService
import io.github.pm665.telegramboot.drivers.scheduling.AbstractBotRecurringJob
import io.micrometer.observation.annotation.Observed
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

class TracingResetPhase1Test {
    private val customObservabilityConfigurationClassName =
        "io.github.pm665.telegramboot.infrastructure.observability.TelegramBootObservabilityConfiguration"

    @Test
    fun `bot action executor service does not declare observed entrypoint annotation`() {
        val method =
            BotActionExecutorService::class.java.getMethod(
                "executeActionForCommandAndState",
                CommandAndState::class.java,
                String::class.java,
                BotUpdate::class.java,
            )

        assertThat(method.getAnnotation(Observed::class.java)).isNull()
    }

    @Test
    fun `abstract bot recurring job does not declare observed entrypoint annotation`() {
        val method = AbstractBotRecurringJob::class.java.getMethod("executeJob")

        assertThat(method.getAnnotation(Observed::class.java)).isNull()
    }

    @Test
    fun `telegram boot configuration does not import custom observability configuration`() {
        val importedConfigurations =
            TelegramBootConfiguration::class.java
                .getAnnotation(Import::class.java)
                .value
                .map { it.java.name }

        assertThat(importedConfigurations).doesNotContain(customObservabilityConfigurationClassName)
    }
}
