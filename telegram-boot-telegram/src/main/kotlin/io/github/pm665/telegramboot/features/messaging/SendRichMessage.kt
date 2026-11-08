package io.github.pm665.telegramboot.features.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException

// ponytail: remove after telegrambots exposes Bot API sendRichMessage.
class SendRichMessage(
    @get:JsonProperty("chat_id") val chatId: String,
    @get:JsonProperty("rich_message") val richMessage: InputRichMessage,
) : BotApiMethodMessage() {
    override fun getMethod() = "sendRichMessage"

    override fun validate() {
        if (chatId.isBlank() || richMessage.html.isBlank()) {
            throw TelegramApiValidationException("Chat ID and rich HTML must not be blank", this)
        }
    }
}

data class InputRichMessage(val html: String)
