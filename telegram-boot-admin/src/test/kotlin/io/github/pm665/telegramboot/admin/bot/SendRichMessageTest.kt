package io.github.pm665.telegramboot.admin.bot

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendRichMessageTest {
    @Test
    fun `serializes Bot API rich markdown request`() {
        val json =
            ObjectMapper().readTree(
                ObjectMapper().writeValueAsString(
                    SendRichMessage("420", InputRichMessage("| Bot | Health |")),
                ),
            )

        assertThat(json["method"].asText()).isEqualTo("sendRichMessage")
        assertThat(json["chat_id"].asText()).isEqualTo("420")
        assertThat(json["rich_message"]["markdown"].asText()).isEqualTo("| Bot | Health |")
    }

    @Test
    fun `serializes Bot API rich html request`() {
        val json =
            ObjectMapper().readTree(
                ObjectMapper().writeValueAsString(
                    SendRichMessage("420", InputRichMessage(html = "<tg-button-row/>")),
                ),
            )

        assertThat(json["rich_message"]["html"].asText()).isEqualTo("<tg-button-row/>")
        assertThat(json["rich_message"].has("markdown")).isFalse()
    }
}
