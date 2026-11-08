package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.events.AdminBotActionEvent
import io.github.pm665.telegramboot.events.AdminBotCommand
import io.github.pm665.telegramboot.events.AdminBotMessageSentEvent
import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AdminBot(
    properties: TelegramAdminProperties,
    private val telegramClient: TelegramClient,
    private val usersService: UsersService,
    private val permissionsService: PermissionsService,
    private val eventPublisher: ApplicationEventPublisher = ApplicationEventPublisher {},
    private val actionExecutor: Executor = Executors.newVirtualThreadPerTaskExecutor(),
    private val adminCommands: List<AdminBotCommand> = emptyList(),
    private val navigation: AdminBotNavigation = AdminBotNavigation(),
) {
    val botUsername = properties.telegramLoginBotUsername.trim()
    val secretToken = secretFor(properties.telegramLoginBotToken.trim())

    // ponytail: process-local history; persist only if replacement must survive restarts.
    private val previousHealthMessages = ConcurrentHashMap<Long, SentMessage>()

    fun registerWebhook(botBaseUrl: String) {
        telegramClient.execute(
            SetWebhook
                .builder()
                .url(webhookUrl(botBaseUrl, botUsername))
                .secretToken(secretToken)
                .build(),
        )
    }

    fun registerCommandsMenu(chatIds: Collection<Long>) {
        telegramClient.execute(menuButton())
        chatIds.forEach { chatId -> telegramClient.execute(menuButton(chatId)) }
    }

    fun handle(update: Update) {
        val callback = update.callbackQuery
        val message = update.message
        val from = callback?.from ?: message?.from ?: return
        val chat = callback?.message?.chat ?: message?.chat ?: return
        if (!chat.isUserChat) return

        val user = usersService.getOrCreateUser(botUsername, from.id, from.userName, from.languageCode ?: "en")
        val serviceAdmin = permissionsService.isUserServiceAdmin(user)
        if (message != null && isStartCommand(message.text)) {
            if (serviceAdmin) usersService.getOrCreateNewChat(user.userId, botUsername, message.chatId)
            if (serviceAdmin) {
                val sent = telegramClient.execute(
                    SendRichMessage(
                        message.chatId.toString(),
                        InputRichMessage(html = startMessage()),
                    ),
                )
                sent.messageId?.takeIf { it > 0 }?.let { navigation.start(message.chatId, it) }
            } else {
                telegramClient.execute(SendMessage.builder().chatId(message.chatId).text("No access").build())
            }
            return
        }
        if (!serviceAdmin) return

        usersService.getOrCreateNewChat(user.userId, botUsername, chat.id)
        val resolved =
            navigation.resolve(
                AdminBotActionEvent(
                    userId = user.userId,
                    chatId = chat.id,
                    message = message?.text,
                    callbackData = callback?.data,
                    sourceMessageId = callback?.message?.messageId,
                ),
        )
        val action = resolved.action
        val cachedResponse = resolved.cachedResponse
        if (action.navigationBack && action.callbackData == AdminBotNavigation.START_CALLBACK) {
            telegramClient.execute(EditRichMessage(action.chatId.toString(), requireNotNull(action.sourceMessageId), InputRichMessage(html = startMessage())))
            return
        }
        if (action.navigationBack && cachedResponse != null) {
            callback?.let {
                telegramClient.execute(
                    AnswerCallbackQuery.builder().callbackQueryId(it.id).text(callbackNotification(action.callbackData)).build(),
                )
            }
            onSendMessage(
                cachedResponse.copy(
                    recipientsUserIds = listOf(action.userId),
                    editChatId = action.chatId,
                    editMessageId = action.sourceMessageId,
                ),
            )
            return
        }
        adminCommands.firstOrNull { action.callbackData?.substringBefore(' ')?.substringBefore('@') in it.commands }?.let {
            navigation.track(it, action)
        }
        callback?.let {
            telegramClient.execute(
                AnswerCallbackQuery.builder().callbackQueryId(it.id).text(callbackNotification(action.callbackData)).build(),
            )
        }
        actionExecutor.execute {
            eventPublisher.publishEvent(action)
        }
    }

    private fun callbackNotification(callbackData: String?): String {
        val command = callbackData?.trim()?.substringBefore(' ')?.substringBefore('@').orEmpty()
        val name =
            adminCommands
                .firstOrNull { command in it.commands }
                ?.name
                ?: command
        return "Получена команда $name"
    }

    private fun startMessage(): String =
        buildString {
            append("<h1>Hi</h1>")
            adminCommands
                .filter { it.showInStartMenu }
                .forEach { command ->
                    append("<tg-button-row align=\"left\"><tg-button type=\"callback_data\" style=\"primary\" data=\"")
                        .append(command.commands.first())
                        .append("\">")
                        .append(command.emoji)
                        .append(' ')
                        .append(command.name)
                        .append("</tg-button></tg-button-row>")
                }
        }

    @EventListener
    fun onSendMessage(event: SendAdminMessageEvent) {
        val recipientIds =
            event.recipientsUserIds.ifEmpty {
                permissionsService.findServiceAdminUserIds()
        }
        usersService.getChats(botUsername, recipientIds).forEach { chat ->
            runCatching {
                sendMessage(
                    navigation.withBackButton(
                        chat.chatId,
                        navigation.prepare(chat.userId, chat.chatId, event),
                    ),
                    chat,
                )
            }
                .onFailure { error ->
                    if (event.tag == SendAdminMessageEvent.HEALTH_TAG) {
                        previousHealthMessages.remove(chat.userId)
                    }
                    log.warn("Failed to send admin bot message tagged {} to user {}", event.tag, chat.userId, error)
                }
        }
    }

    private fun sendMessage(
        event: SendAdminMessageEvent,
        chat: UsersService.Chat,
    ) {
        val richEvent = event.withRichButtons()
        if (richEvent.editMessageId != null && richEvent.editChatId == chat.chatId) {
            editMessage(richEvent)
            return
        }
        if (richEvent.tag == SendAdminMessageEvent.HEALTH_TAG) {
            previousHealthMessages[chat.userId]?.let { previous ->
                telegramClient.execute(
                    DeleteMessage
                        .builder()
                        .chatId(previous.chatId)
                        .messageId(previous.messageId)
                        .build(),
                )
            }
        }
        val sent =
            if (richEvent.richHtml != null || (richEvent.buttons.isEmpty() && richEvent.tag != SendAdminMessageEvent.HEALTH_TAG)) {
                telegramClient.execute(
                    SendRichMessage(
                        chat.chatId.toString(),
                        richEvent.richHtml?.let { InputRichMessage(html = it) } ?: InputRichMessage(markdown = richEvent.message),
                    ),
                )
            } else {
                telegramClient.execute(plainMessage(richEvent, chat.chatId))
            }
        if (event.tag == SendAdminMessageEvent.HEALTH_TAG) {
            previousHealthMessages[chat.userId] = SentMessage(chat.chatId, sent.messageId)
            sent.messageId?.takeIf { it > 0 }?.let { navigation.start(chat.chatId, it, "/health", richEvent) }
        }
        val requestId = event.requestId
        val messageId = sent.messageId
        if (requestId != null && messageId != null) {
            eventPublisher.publishEvent(AdminBotMessageSentEvent(requestId, chat.userId, chat.chatId, messageId))
        }
        event.deleteMessageId?.let { messageId ->
            telegramClient.execute(DeleteMessage.builder().chatId(chat.chatId).messageId(messageId).build())
        }
    }

    private fun editMessage(event: SendAdminMessageEvent) {
        val chatId = requireNotNull(event.editChatId).toString()
        val messageId = requireNotNull(event.editMessageId)
        if (event.richHtml != null) {
            telegramClient.execute(EditRichMessage(chatId, messageId, InputRichMessage(html = event.richHtml)))
            return
        }
        telegramClient.execute(
            EditMessageText
                .builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(event.message)
                .apply { if (event.buttons.isNotEmpty()) replyMarkup(keyboard(event)) }
                .build(),
        )
    }

    private fun plainMessage(
        event: SendAdminMessageEvent,
        chatId: Long,
    ): SendMessage =
        SendMessage
            .builder()
            .chatId(chatId)
            .text(event.message)
            .apply {
                if (event.buttons.isNotEmpty()) {
                    replyMarkup(keyboard(event))
                }
            }.build()

    private fun SendAdminMessageEvent.withRichButtons(): SendAdminMessageEvent {
        if (buttons.isEmpty()) return this
        val richButtons = buttons.joinToString("") { button ->
            "<tg-button-row align=\"left\"><tg-button type=\"callback_data\" style=\"primary\" data=\"${button.callbackData.htmlEscape()}\">${button.text.htmlEscape()}</tg-button></tg-button-row>"
        }
        return copy(
            buttons = emptyList(),
            richHtml = (richHtml ?: "<p>${message.htmlEscape().replace("\n", "<br/>")}</p>") + richButtons,
        )
    }

    private fun String.htmlEscape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun keyboard(event: SendAdminMessageEvent): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(
                event.buttons.map { button ->
                    InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(button.text).callbackData(button.callbackData).build(),
                    )
                },
            ).build()

    private fun isStartCommand(text: String?): Boolean {
        val command = text?.trim()?.split(Regex("\\s+"), limit = 2)?.firstOrNull()
        return command == "/start" || command == "/start@$botUsername"
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminBot::class.java)

        fun secretFor(botToken: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(botToken.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        private fun webhookUrl(
            botBaseUrl: String,
            botUsername: String,
        ): String {
            require(botUsername.isNotBlank()) {
                "telegram-boot.admin.telegram-login-bot-username must not be blank"
            }
            val uri = runCatching { URI(botBaseUrl.trim()) }.getOrNull()
            require(
                uri?.scheme == "https" &&
                    !uri.host.isNullOrBlank() &&
                    uri.path.isNullOrEmpty(),
            ) {
                "telegram-boot.admin.bot-base-url must be an absolute HTTPS URL"
            }
            return URI(uri.scheme, uri.authority, "/bot/$botUsername", null, null).toString()
        }

        private fun menuButton(chatId: Long? = null): SetChatMenuButton =
            SetChatMenuButton
                .builder()
                .menuButton(MenuButtonCommands())
                .apply {
                    chatId?.let { chatId(it) }
                }.build()
    }

    private data class SentMessage(
        val chatId: Long,
        val messageId: Int,
    )
}
