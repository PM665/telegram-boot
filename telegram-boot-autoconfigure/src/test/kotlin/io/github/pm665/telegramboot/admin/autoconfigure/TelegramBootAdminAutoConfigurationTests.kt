package io.github.pm665.telegramboot.admin.autoconfigure

import io.github.pm665.telegramboot.admin.TelegramBootAdminConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

class TelegramBootAdminAutoConfigurationTests {
    @Test
    fun `imports shared admin configuration`() {
        val import = TelegramBootAdminAutoConfiguration::class.java.getAnnotation(Import::class.java)

        assertThat(import.value).containsExactly(TelegramBootAdminConfiguration::class)
    }
}
