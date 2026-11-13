package io.github.pm665.telegramboot.admin.bot

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException

// ponytail: remove after telegrambots exposes Bot API sendRichMessage.
@JsonInclude(JsonInclude.Include.NON_NULL)
class SendRichMessage(
    @get:JsonProperty("chat_id") val chatId: String,
    @get:JsonProperty("rich_message") val richMessage: InputRichMessage,
) : BotApiMethodMessage() {
    override fun getMethod() = "sendRichMessage"

    override fun validate() {
        if (chatId.isBlank()) {
            throw TelegramApiValidationException("Chat ID must not be blank", this)
        }
        if (richMessage.html.isBlank()) {
            throw TelegramApiValidationException("Rich message HTML must not be blank", this)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class EditRichMessage(
    @get:JsonProperty("chat_id") val chatId: String,
    @get:JsonProperty("message_id") val messageId: Int,
    @get:JsonProperty("rich_message") val richMessage: InputRichMessage,
) : BotApiMethodMessage() {
    override fun getMethod() = "editMessageText"

    override fun validate() {
        if (chatId.isBlank() || messageId <= 0) {
            throw TelegramApiValidationException("Chat ID and message ID must be set", this)
        }
    }
}

data class InputRichMessage(
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    @get:JsonProperty("html") val html: String,
)
