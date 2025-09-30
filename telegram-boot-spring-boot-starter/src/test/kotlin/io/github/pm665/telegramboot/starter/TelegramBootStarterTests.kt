package io.github.pm665.telegramboot.starter

import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource

class TelegramBootStarterTests {
    @Test
    fun `auto configuration imports file exposes telegram boot configuration`() {
        val resource = ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")

        assertThat(resource.exists()).isTrue()

        val entries =
            resource.inputStream.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.trim().startsWith("#") }.toList()
            }

        assertThat(entries)
            .contains("io.github.pm665.telegramboot.bot.autoconfigure.TelegramBootServiceAutoConfiguration")
    }

    @Test
    fun `starter makes telegram boot service auto configuration available`() {
        val resource = ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
        val autoConfigurationClasses =
            resource.inputStream.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.trim().startsWith("#") }
                    .map { Class.forName(it) }
                    .toList()
            }

        val runner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(*autoConfigurationClasses.toTypedArray()))
                .withPropertyValues("telegram-boot.enabled=true")

        runner.run { context ->
            assertThat(context).hasSingleBean(TelegramBootService::class.java)
        }
    }
}
