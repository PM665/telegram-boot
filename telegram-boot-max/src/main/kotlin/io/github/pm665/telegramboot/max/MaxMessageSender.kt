package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.JsonNode
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.PlatformMessageSender
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory

internal class MaxMessageSender(
    private val botsService: BotsService,
    private val apiClient: MaxApiClient,
    private val usersService: UsersService,
    private val messageTextProcessor: MessageTextProcessor,
    private val rateLimiter: MaxMessageRateLimiter,
) : PlatformMessageSender {
    private val log = LoggerFactory.getLogger(javaClass)
    override val platform = BotPlatform.MAX

    override fun sendMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
        messageArgs: Map<String, String>,
    ): BotMessage? = sendMessageWithArgs(sendMessageMarkup, chatId, botUsername, messageArgs)

    private fun sendMessageWithArgs(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
        messageArgs: Map<String, *>,
    ): BotMessage? {
        if (sendMessageMarkup.photo != null) {
            log.warn("Skipping media for MAX bot={} chatId={}; MAX media delivery is not enabled", botUsername, chatId)
        }
        if (sendMessageMarkup.replyKeyboard.isNotEmpty()) {
            log.warn("Skipping Telegram reply keyboard for MAX bot={} chatId={}; MAX uses inline keyboards", botUsername, chatId)
        }
        val text = prepareText(sendMessageMarkup.text, botUsername, chatId, messageArgs)
        require(text.codePointCount(0, text.length) <= MAX_TEXT_LENGTH) { "MAX message text exceeds $MAX_TEXT_LENGTH characters" }
        if (text.isBlank() && sendMessageMarkup.menuMarkup == null && !sendMessageMarkup.requestLocation) return null
        val body =
            mutableMapOf<String, Any>(
                "text" to text,
                "format" to "markdown",
            )
        if (sendMessageMarkup.menuMarkup != null || sendMessageMarkup.requestLocation) {
            body["attachments"] =
                listOf(inlineKeyboard(sendMessageMarkup.menuMarkup, sendMessageMarkup.requestLocation, botUsername, chatId, messageArgs))
        }
        val botToken = token(botUsername)
        rateLimiter.waitForTurn(botUsername, chatId)
        return apiClient.sendMessage(botToken, chatId, body).toBotMessage(chatId)
    }

    override fun sendLocation(
        lat: Double,
        lon: Double,
        chatId: Long,
        botUsername: String,
    ): BotMessage? {
        val botToken = token(botUsername)
        rateLimiter.waitForTurn(botUsername, chatId)
        return apiClient
            .sendMessage(
                botToken,
                chatId,
                mapOf("attachments" to listOf(mapOf("type" to "location", "latitude" to lat, "longitude" to lon))),
            ).toBotMessage(chatId)
    }

    override fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String>,
        botUsername: String,
    ) = sendCallbackNotification(update, text, messageArgs, botUsername, false)

    override fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String>,
        botUsername: String,
        showAlert: Boolean,
    ) {
        val callback = update.callbackQuery ?: return
        if (text.isNotBlank() || showAlert) {
            log.warn("MAX callback toast notifications are not mapped; acknowledging callback for bot={}", botUsername)
        }
        apiClient.answerCallback(token(botUsername), callback.id)
    }

    override fun deleteCallbackSource(
        update: BotUpdate?,
        botUsername: String,
    ) {
        val message = update?.callbackQuery?.message ?: return
        val messageId = message.messageId ?: return
        val chatId = message.chat?.id ?: return
        deleteMessage(messageId, chatId, botUsername)
    }

    override fun deleteMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) {
        if (messageId == null) return
        apiClient.deleteMessage(token(botUsername), messageId)
    }

    override fun pinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) = log.warn("Pinning MAX messages is not implemented for bot={} chatId={}", botUsername, chatId)

    override fun unpinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) = log.warn("Unpinning MAX messages is not implemented for bot={} chatId={}", botUsername, chatId)

    override fun sendMessages(context: MessageSender.SendMessageContext): Collection<BotMessage> {
        val botUsername = context.botUsername ?: return emptyList()
        return context.chatId.flatMap { chatId ->
            context.messagesToSend.mapNotNull { markup ->
                sendMessageWithArgs(markup, chatId, botUsername, context.messageArgs)
            } + context.locationsToSend.mapNotNull { sendLocation(it.lat, it.lon, chatId, botUsername) }
        }
    }

    override fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
    ) {
        val callback = update.callbackQuery ?: return
        val chatId = callback.message?.chat?.id ?: return
        val body = mutableMapOf<String, Any>()
        if (text.isNotEmpty()) {
            val preparedText = prepareText(text, botUsername, chatId, messageArgs)
            require(preparedText.codePointCount(0, preparedText.length) <= MAX_TEXT_LENGTH) {
                "MAX message text exceeds $MAX_TEXT_LENGTH characters"
            }
            body["text"] = preparedText
        }
        inlineKeyboard?.let { body["attachments"] = listOf(inlineKeyboard(it, false, botUsername, chatId, messageArgs)) }
        apiClient.answerCallback(token(botUsername), callback.id, body)
    }

    override fun sendCommands(
        botUsername: String,
        commands: Map<String, String>,
    ) {
        require(commands.size <= MAX_COMMANDS) { "MAX supports at most $MAX_COMMANDS commands" }
        val maxCommands =
            commands.map { (name, descriptionKey) ->
                val description = messageTextProcessor.prepareText(descriptionKey, MessageTextProcessor.DEFAULT_LOCALE)
                require(name.codePointCount(0, name.length) in 1..MAX_COMMAND_NAME_LENGTH) {
                    "MAX command name exceeds $MAX_COMMAND_NAME_LENGTH characters"
                }
                require(description.codePointCount(0, description.length) in 1..MAX_COMMAND_DESCRIPTION_LENGTH) {
                    "MAX command description exceeds $MAX_COMMAND_DESCRIPTION_LENGTH characters"
                }
                mapOf("name" to name, "description" to description)
            }
        apiClient.setCommands(token(botUsername), maxCommands)
    }

    private fun inlineKeyboard(
        markup: MenuMarkup?,
        requestLocation: Boolean,
        botUsername: String,
        chatId: Long,
        messageArgs: Map<String, *>,
    ): Map<String, Any> {
        val rows =
            markup?.rows?.map { row ->
                row.buttons.map { button ->
                    val text = prepareText(button.text, botUsername, chatId, messageArgs)
                    if (button.url != null) {
                        mapOf("type" to "link", "text" to text, "url" to button.url)
                    } else {
                        mapOf("type" to "callback", "text" to text, "payload" to button.callbackData.orEmpty())
                    }
                }
            }?.toMutableList() ?: mutableListOf()
        if (requestLocation) {
            val locationText = prepareText("common.shareLocation", botUsername, chatId, messageArgs)
            rows += listOf(mapOf("type" to "request_geo_location", "text" to locationText))
        }
        validateKeyboard(rows)
        return mapOf(
            "type" to "inline_keyboard",
            "payload" to mapOf("buttons" to rows),
        )
    }

    private fun validateKeyboard(rows: List<List<Map<String, String?>>>) {
        require(rows.size <= MAX_KEYBOARD_ROWS) { "MAX keyboard exceeds $MAX_KEYBOARD_ROWS rows" }
        require(rows.sumOf { it.size } <= MAX_KEYBOARD_BUTTONS) {
            "MAX keyboard exceeds $MAX_KEYBOARD_BUTTONS buttons"
        }
        rows.forEach { row ->
            val hasSpecialButton = row.any { it["type"] in MAX_SPECIAL_BUTTON_TYPES }
            val maxButtons = if (hasSpecialButton) MAX_SPECIAL_BUTTONS_PER_ROW else MAX_BUTTONS_PER_ROW
            require(row.size <= maxButtons) { "MAX keyboard row exceeds $maxButtons buttons" }
            row.filter { it["type"] == "link" }.forEach { button ->
                require(button["url"].orEmpty().length <= MAX_LINK_LENGTH) { "MAX keyboard link exceeds $MAX_LINK_LENGTH characters" }
            }
        }
    }

    private fun prepareText(
        text: String,
        botUsername: String,
        chatId: Long,
        messageArgs: Map<String, *>,
    ): String {
        val locale = usersService.getUserChat(botUsername, chatId)?.user?.locale ?: MessageTextProcessor.DEFAULT_LOCALE
        return messageTextProcessor.prepareText(text, locale, messageArgs)
    }

    private fun token(botUsername: String): String =
        botsService.findBotByBotUsername(botUsername)?.takeIf { it.platform == platform }?.token
            ?: throw IllegalArgumentException("MAX bot not found: $botUsername")

    private fun JsonNode.toBotMessage(chatId: Long): BotMessage {
        val message = path("message").takeUnless(JsonNode::isMissingNode) ?: this
        val messageId = message.path("body").path("mid").asText(null) ?: error("MAX response has no message id")
        val responseChatId = message.path("recipient").path("chat_id").takeUnless(JsonNode::isMissingNode)?.asLong() ?: chatId
        return BotMessage(messageId, responseChatId)
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 4000
        private const val MAX_KEYBOARD_ROWS = 30
        private const val MAX_KEYBOARD_BUTTONS = 210
        private const val MAX_BUTTONS_PER_ROW = 7
        private const val MAX_SPECIAL_BUTTONS_PER_ROW = 3
        private const val MAX_LINK_LENGTH = 2048
        private const val MAX_COMMANDS = 32
        private const val MAX_COMMAND_NAME_LENGTH = 64
        private const val MAX_COMMAND_DESCRIPTION_LENGTH = 128
        private val MAX_SPECIAL_BUTTON_TYPES = setOf("link", "open_app", "request_geo_location", "request_contact")
    }
}
