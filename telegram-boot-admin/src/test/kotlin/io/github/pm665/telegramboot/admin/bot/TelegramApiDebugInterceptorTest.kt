package io.github.pm665.telegramboot.admin.bot

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TelegramApiDebugInterceptorTest {
    @Test
    fun `observes only rich message requests`() {
        assertThat(TelegramApiDebugInterceptor.observes("sendRichMessage")).isTrue()
        assertThat(TelegramApiDebugInterceptor.observes("editMessageText")).isTrue()
        assertThat(TelegramApiDebugInterceptor.observes("sendMessage")).isFalse()
    }
}
