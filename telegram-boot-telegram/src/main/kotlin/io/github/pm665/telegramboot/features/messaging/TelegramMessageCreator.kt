package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.features.callbacks.CallbackData
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@Component
class TelegramMessageCreator(
    private val usersService: UsersService,
    private val callbackDataService: CallbackDataService,
    private val messageTextProcessor: MessageTextProcessor,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TelegramMessageCreator::class.java)
        private const val TELEGRAM_CALLBACK_DATA_LIMIT_BYTES = 63
    }

    fun createBotApiMessage(
        sendMessageMarkup: SendMessageMarkup,
        messageArgs: Map<String, Any?>,
        chatId: Long,
        botUsername: String,
        silent: Boolean = false,
    ): PartialBotApiMethod<Message> {
        validateMessageMarkup(sendMessageMarkup)
        val userChat = usersService.getUserChat(botUsername, chatId)
        var locale = userChat?.user?.locale
        if (locale == null) {
            locale = MessageTextProcessor.DEFAULT_LOCALE
        }
        val messageText = messageTextProcessor.prepareText(sendMessageMarkup.text, locale, messageArgs)
        val menu = sendMessageMarkup.menuMarkup
        if (menu?.rich == true) {
            val html = buildString {
                append("<p>").append(messageText.htmlEscape()).append("</p>")
                menu.rows.forEach { row ->
                    append("<tg-button-row align=\"left\">")
                    row.buttons.forEach { button ->
                        val text = messageTextProcessor.prepareText(button.text, locale, messageArgs)
                        val callback = requireNotNull(resolveCallbackData(button.callbackData))
                        append("<tg-button type=\"callback_data\" style=\"primary\" data=\"")
                            .append(callback.htmlEscape()).append("\">").append(text.htmlEscape()).append("</tg-button>")
                    }
                    append("</tg-button-row>")
                }
            }
            return SendRichMessage(chatId.toString(), InputRichMessage(html))
        }
        val markup = createReplyKeyboard(sendMessageMarkup, messageArgs, locale)
        val photo = sendMessageMarkup.photo
        if (photo != null) {
            return createPhotoMessage(
                sendMessageMarkup = sendMessageMarkup,
                chatId = chatId,
                silent = silent,
                messageText = messageText,
                markup = markup,
                photo = photo,
            )
        } else {
            val builder =
                SendMessage
                    .builder()
                    .text(messageText)
                    .chatId(chatId)
                    .disableNotification(silent)
            sendMessageMarkup.replyToMessageId?.toIntOrNull()?.let(builder::replyToMessageId)
            markup?.let { builder.replyMarkup(markup) }

            val msg = builder.build()
            msg.enableHtml(true)
            return msg
        }
    }

    private fun String.htmlEscape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun createPhotoMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        silent: Boolean,
        messageText: String,
        markup: ReplyKeyboard? = null,
        photo: SendMessageMarkup.Photo,
    ): SendPhoto {
        val inputFile =
            when (photo) {
                is SendMessageMarkup.Photo.Bytes -> {
                    require(photo.filename.isNotBlank()) { "photo filename must not be blank" }
                    require(photo.contentType.isNotBlank()) { "photo contentType must not be blank" }
                    require(photo.contentType.startsWith("image/")) { "photo contentType must be image/*" }
                    require(photo.bytes.isNotEmpty()) { "photo bytes must not be empty" }
                    InputFile(ByteArrayInputStream(photo.bytes), photo.filename)
                }

                is SendMessageMarkup.Photo.ProviderFileId -> {
                    require(photo.value.isNotBlank()) { "photo file id must not be blank" }
                    InputFile(photo.value)
                }
            }
        val builder =
            SendPhoto
                .builder()
                .chatId(chatId)
                .caption(messageText)
                .photo(inputFile)
                .disableNotification(silent)
                .replyMarkup(markup)
                .parseMode("HTML")
        sendMessageMarkup.replyToMessageId?.toIntOrNull()?.let(builder::replyToMessageId)
        return builder.build()
    }

    private fun validateMessageMarkup(sendMessageMarkup: SendMessageMarkup) {
        require(sendMessageMarkup.photo == null || !sendMessageMarkup.isRequestLocation) {
            "photo message must not request location"
        }
    }

    fun createInlineKeyboardMarkup(
        inlineKeyboard: MenuMarkup,
        messageArgs: Map<String, Any?>,
        locale: String,
    ): InlineKeyboardMarkup {
        val keyboardBuilder = InlineKeyboardMarkup.builder()
        inlineKeyboard.rows.forEach { row ->
            val buttons =
                row.buttons.map { btn ->
                    val text = messageTextProcessor.prepareText(btn.text, locale, messageArgs)
                    val resolvedCallback = resolveCallbackData(btn.callbackData)
                    InlineKeyboardButton
                        .builder()
                        .text(text)
                        .url(btn.url)
                        .callbackData(resolvedCallback)
                        .build()
                }
            keyboardBuilder.keyboardRow(InlineKeyboardRow(buttons))
        }
        return keyboardBuilder.build()
    }

    private fun createReplyKeyboard(
        sendMessageMarkup: SendMessageMarkup,
        messageArgs: Map<String, Any?>,
        locale: String,
    ): ReplyKeyboard? {
        val replyKeyboard = sendMessageMarkup.replyKeyboard
        val menuMarkup = sendMessageMarkup.menuMarkup
        return when {
            sendMessageMarkup.isRequestLocation -> {
                createLocationRequestKeyboard(messageArgs, locale)
            }

            !CollectionUtils.isEmpty(replyKeyboard) -> {
                createReplyKeyboardMarkup(messageArgs, locale, replyKeyboard)
            }

            menuMarkup != null -> {
                createInlineKeyboardMarkup(menuMarkup, messageArgs, locale)
            }

        sendMessageMarkup.isPhoto -> {
                null
            }

            else -> {
                ReplyKeyboardRemove.builder().removeKeyboard(true).build()
            }
        }
    }

    private fun createReplyKeyboardMarkup(
        messageArgs: Map<String, Any?>,
        locale: String,
        replyKeyboard: List<String>,
    ): ReplyKeyboardMarkup {
        val markupBuilder = ReplyKeyboardMarkup.builder()
        replyKeyboard.forEach { text ->
            val btn = KeyboardButton.builder().text(messageTextProcessor.prepareText(text, locale, messageArgs)).build()
            markupBuilder.keyboardRow(KeyboardRow(ArrayList(listOf(btn))))
        }
        return markupBuilder.build()
    }

    private fun createLocationRequestKeyboard(
        messageArgs: Map<String, Any?>,
        locale: String,
    ): ReplyKeyboardMarkup {
        val locationButton =
            KeyboardButton
                .builder()
                .requestLocation(true)
                .text(messageTextProcessor.prepareText("common.shareLocation", locale, messageArgs))
                .build()
        val row = KeyboardRow(locationButton)

        return ReplyKeyboardMarkup
            .builder()
            .resizeKeyboard(true)
            .oneTimeKeyboard(true)
            .keyboardRow(row)
            .build()
    }

    private fun resolveCallbackData(callback: String?): String? {
        if (callback.isNullOrBlank()) {
            return callback
        }

        if (callback.toByteArray(StandardCharsets.UTF_8).size <= TELEGRAM_CALLBACK_DATA_LIMIT_BYTES) {
            return callback
        }

        val callbackDataRecord = CallbackData.fromData(callback)
        val callbackId = callbackDataRecord.id
        val existing = callbackDataService.findById(callbackId)
        if (existing != null) {
            if (callback != existing.data) {
                log.warn("Callback data hash collision for id {}", callbackId)
                return callback
            }
            return callbackId
        }

        callbackDataService.createNew(callbackDataRecord)
        return callbackId
    }
}
