package io.github.pm665.telegramboot.domain.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class TelegramBootPropertiesTest {
    @Test
    fun `defaults are applied`() {
        val properties = TelegramBootProperties()

        assertThat(properties.name).isEqualTo("telegramBoot")
        assertThat(properties.enabled).isTrue()
    }

    @Test
    fun `binds custom configuration`() {
        val source =
            MapConfigurationPropertySource(
                mapOf(
                    "telegram-boot.name" to "custom-bot",
                    "telegram-boot.enabled" to "false",
                ),
            )

        val binder = Binder(source)

        val bound = binder.bind("telegram-boot", Bindable.of(TelegramBootProperties::class.java)).get()

        assertThat(bound.name).isEqualTo("custom-bot")
        assertThat(bound.enabled).isFalse()
    }
}
