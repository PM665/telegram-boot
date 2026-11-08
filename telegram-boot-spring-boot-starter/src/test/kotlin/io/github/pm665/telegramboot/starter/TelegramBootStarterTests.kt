package io.github.pm665.telegramboot.starter

import io.github.pm665.telegramboot.TelegramBootTelegramAutoConfiguration
import io.github.pm665.telegramboot.core.autoconfigure.TelegramBootCoreAutoConfiguration
import io.github.pm665.telegramboot.drivers.telegram.BotsFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.Collections

class TelegramBootStarterTests {
    @Test
    fun `auto configuration imports file exposes telegram boot configuration`() {
        val entries = autoConfigurationEntries()

        assertThat(entries)
            .contains("io.github.pm665.telegramboot.core.autoconfigure.TelegramBootCoreAutoConfiguration")
            .contains("io.github.pm665.telegramboot.TelegramBootTelegramAutoConfiguration")
    }

    @Test
    fun `starter makes telegram boot service auto configuration available`() {
        val runner =
            ApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        TelegramBootCoreAutoConfiguration::class.java,
                        TelegramBootTelegramAutoConfiguration::class.java,
                    ),
                )
                .withPropertyValues("telegram-boot.enabled=true")

        runner.run { context ->
            assertThat(context).hasSingleBean(BotsFactory::class.java)
            }
    }

    private fun autoConfigurationEntries(): List<String> =
        Collections
            .list(
                javaClass.classLoader.getResources(
                    "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                ),
            ).flatMap { resource ->
                resource.openStream().bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.trim().startsWith("#") }.toList()
                }
            }
}
