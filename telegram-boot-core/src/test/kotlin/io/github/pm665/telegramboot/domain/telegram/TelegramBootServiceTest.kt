package io.github.pm665.telegramboot.domain.telegram

import io.github.pm665.telegramboot.domain.configuration.TelegramBootProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class TelegramBootServiceTest {
    @Test
    fun `returns configured bot name`() {
        val properties = TelegramBootProperties().apply { name = "test-bot" }

        val service = TelegramBootService(properties)

        assertThat(service.botName()).isEqualTo("test-bot")
    }

    @Test
    fun `logs configured bot name on initialization`(capturedOutput: CapturedOutput) {
        val properties = TelegramBootProperties().apply { name = "logging-bot" }

        TelegramBootService(properties)

        assertThat(capturedOutput.out)
            .contains("Telegram boot 'logging-bot' is ready to receive updates")
    }
}
