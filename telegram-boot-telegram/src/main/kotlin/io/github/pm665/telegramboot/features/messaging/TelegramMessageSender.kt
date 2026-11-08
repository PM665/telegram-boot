package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.bot.BotFileReference
import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.drivers.telegram.OperatorBot
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.PlatformMessageSender
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage
import org.telegram.telegrambots.meta.api.methods.send.SendLocation
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable

@Service
class TelegramMessageSender(
    private val applicationContext: ApplicationContext,
    private val usersService: UsersService,
    private val messageTextProcessor: MessageTextProcessor,
    private val telegramMessageCreator: TelegramMessageCreator,
) : PlatformMessageSender {
    override val platform = BotPlatform.TELEGRAM

    private val log = LoggerFactory.getLogger(TelegramMessageSender::class.java)

    init {
        log.info("TelegramMessageSender initialized")
        log.info("ApplicationContext class: {}", applicationContext.javaClass.name)
        log.info("UsersService class: {}", usersService.javaClass.name)
        log.info("MessageTextProcessor class: {}", messageTextProcessor.javaClass.name)
        log.info("TelegramMessageCreator class: {}", telegramMessageCreator.javaClass.name)
    }

    override fun sendMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
        messageArgs: Map<String, String>,
    ): BotMessage? {
        val method = telegramMessageCreator.createBotApiMessage(sendMessageMarkup, messageArgs, chatId, botUsername)
        return sendPartialMessageInternal(method, botUsername)?.toBotMessage()
    }

    override fun sendLocation(
        lat: Double,
        lon: Double,
        chatId: Long,
        botUsername: String,
    ): BotMessage? {
        val msg =
            SendLocation
                .builder()
                .latitude(lat)
                .longitude(lon)
                .chatId(chatId)
                .build()
        msg.enableNotification()
        return sendBotApiMethodInternal(msg, botUsername)?.toBotMessage()
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
        if (ChatUtils.Flag.CALLBACK_QUERY.test(update)) {
            val callbackQuery = requireNotNull(update.callbackQuery) { "Callback query is required" }
            val callbackQueryId = callbackQuery.id
            val chatUser = ChatUtils.extractUser(update)
            val locale = resolveLocale(chatUser, botUsername, update)
            val answerCallbackQuery =
                AnswerCallbackQuery
                    .builder()
                    .text(prepareText(text, locale, messageArgs))
                    .callbackQueryId(callbackQueryId)
                    .showAlert(showAlert)
                    .build()
            sendBotApiMethodInternal(answerCallbackQuery, botUsername)
        }
    }

    override fun deleteCallbackSource(
        update: BotUpdate?,
        botUsername: String,
    ) {
        if (ChatUtils.Flag.CALLBACK_QUERY.test(update)) {
            val callbackQuery = requireNotNull(update?.callbackQuery) { "Callback query is required" }
            val message = callbackQuery.message ?: return
            val chatId = message.chat?.id ?: return
            val messageId = message.messageId
            deleteMessage(messageId, chatId, botUsername)
        }
    }

    override fun deleteMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) {
        try {
            val telegramMessageId = messageId?.toIntOrNull() ?: return
            val builder = DeleteMessage.builder().chatId(chatId).messageId(telegramMessageId)
            val deleteMessage = builder.build()
            sendBotApiMethodInternal(deleteMessage, botUsername)
        } catch (e: Exception) {
            log.debug("Could not delete message: {} {}", e.javaClass, e.message)
        }
    }

    override fun pinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) {
        try {
            val telegramMessageId = messageId?.toIntOrNull()
            if (telegramMessageId == null) {
                log.debug("pinMessage called with null messageId chatId={} bot={}", chatId, botUsername)
                return
            }
            val pin =
                PinChatMessage
                    .builder()
                    .chatId(chatId)
                    .messageId(telegramMessageId)
                    .build()
            sendBotApiMethodInternal(pin, botUsername)
        } catch (e: Exception) {
            log.debug("Could not pin message: {} {}", e.javaClass, e.message)
        }
    }

    override fun unpinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) {
        try {
            val telegramMessageId = messageId?.toIntOrNull() ?: return
            val builder = UnpinChatMessage.builder().chatId(chatId).messageId(telegramMessageId)
            val message: BotApiMethod<Boolean> = builder.build()
            sendBotApiMethodInternal(message, botUsername)
        } catch (e: Exception) {
            log.debug("Could not unpin message: {} {}", e.javaClass, e.message)
        }
    }

    override fun sendMessages(context: MessageSender.SendMessageContext): Collection<BotMessage> {
        val botUsername = context.botUsername ?: return emptyList()

        val results: MutableList<BotMessage> = mutableListOf()
        for (chatId in context.chatId) {
            val sendMessages =
                context.messagesToSend.map { sendMessageMarkup ->
                    telegramMessageCreator.createBotApiMessage(
                        sendMessageMarkup = sendMessageMarkup,
                        messageArgs = context.messageArgs,
                        chatId = chatId,
                        botUsername = botUsername,
                        silent = context.silent,
                    )
                }

            for (method in sendMessages) {
                val telegramMessage = sendPartialMessageInternal(method, botUsername)
                if (telegramMessage != null) {
                    val message = telegramMessage.toBotMessage()
                    if (context.pin) {
                        pinMessage(message.messageId, message.chatId, botUsername)
                    }
                    results.add(message)
                }
            }
            if (!CollectionUtils.isEmpty(context.locationsToSend)) {
                context.locationsToSend.forEach { location ->
                    val locationMessage = sendLocation(location.lat, location.lon, chatId, botUsername)
                    if (locationMessage != null) results.add(locationMessage)
                }
            }
        }
        return results
    }

    override fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
    ) = editMessage(botUsername, update, text, inlineKeyboard, messageArgs, null)

    override fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
        parseMode: String?,
    ) {
        if (ChatUtils.Flag.CALLBACK_QUERY.test(update)) {
            val callbackMessage = update.callbackQuery?.message ?: return
            val messageId = callbackMessage.messageId?.toIntOrNull() ?: return
            val chatUser = ChatUtils.extractUser(update)
            val locale = resolveLocale(chatUser, botUsername, update)
            if (text.isNotEmpty()) {
                val builder =
                    EditMessageText
                        .builder()
                        .chatId(ChatUtils.extractChatId(update))
                        .messageId(messageId)
                        .text(prepareText(text, locale, messageArgs))
                parseMode?.let(builder::parseMode)
                val textEdit = builder.build()
                sendBotApiMethodInternal(textEdit, botUsername)
            }
            if (inlineKeyboard != null) {
                val markup = telegramMessageCreator.createInlineKeyboardMarkup(inlineKeyboard, messageArgs, locale)
                val edit =
                    EditMessageReplyMarkup
                        .builder()
                        .chatId(ChatUtils.extractChatId(update))
                        .messageId(messageId)
                        .replyMarkup(markup)
                        .build()
                sendBotApiMethodInternal(edit, botUsername)
            }
        }
    }

    private fun resolveLocale(
        chatUser: UsersService.User?,
        botUsername: String,
        update: BotUpdate,
    ): String {
        var locale: String? = null
        if (chatUser != null) {
            val stored = usersService.getUser(botUsername, chatUser.userId)
            if (stored != null) locale = stored.locale
        }
        if (locale == null) {
            locale = getLocale(botUsername, update)
        }
        return locale
    }

    override fun sendCommands(
        botUsername: String,
        commands: Map<String, String>,
    ) {
        val scope = BotCommandScopeDefault.builder().build()
        sendCommands(botUsername, scope, commands)
    }

    private fun sendCommands(
        botUsername: String,
        scope: BotCommandScope,
        commands: Map<String, String>,
    ) {
        for (locale in MessageTextProcessor.SUPPORTED_LOCALES) {
            val commandsList =
                commands.entries.map { entry ->
                    BotCommand(entry.key, messageTextProcessor.prepareText(entry.value, locale))
                }
            val myCommandsBuilder = SetMyCommands.builder().scope(scope).commands(commandsList)
            if (MessageTextProcessor.DEFAULT_LOCALE != locale) myCommandsBuilder.languageCode(locale)
            val message: BotApiMethod<Boolean> = myCommandsBuilder.build()
            sendBotApiMethodInternal(message, botUsername)
        }
    }

    private fun <T : Serializable> sendBotApiMethodInternal(
        message: BotApiMethod<T>,
        botUsername: String,
    ): T? {
        val chatId = extractChatIdFromBotApiMethod(message)
        try {
            log.debug(
                "sendMessageInternal Sending message bot={} chatId={} messageType={}",
                botUsername,
                chatId,
                message.javaClass,
            )
            val bot = applicationContext.getBean<OperatorBot>(botUsername)
            return bot.execute(message)
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is TelegramApiException) {
                if (cause.message?.contains("403") == true && e.message?.contains("bot was blocked by the user") == true) {
                    log.debug("User blocked the bot: bot={} chatId={}", botUsername, chatId)
                    if (chatId > 0) {
                        usersService.disableChat(botUsername, chatId)
                    }
                }
            }
            log.debug(
                "Could not send messages to bot={} exceptionClass={} exceptionMessage=\"{}\", messageToSend={}",
                botUsername,
                e.javaClass,
                e.message,
                message,
            )
        }
        return null
    }

    private fun sendPartialMessageInternal(
        message: PartialBotApiMethod<Message>,
        botUsername: String,
    ): Message? =
        when (message) {
            is SendPhoto -> {
                sendPhotoInternal(message, botUsername)
            }

            is BotApiMethod<*> -> {
                @Suppress("UNCHECKED_CAST")
                sendBotApiMethodInternal(message as BotApiMethod<Message>, botUsername)
            }

            else -> {
                log.debug("Unsupported telegram message type bot={} messageType={}", botUsername, message.javaClass)
                null
            }
        }

    private fun sendPhotoInternal(
        message: SendPhoto,
        botUsername: String,
    ): Message? {
        val chatId = message.chatId.toLongOrNull() ?: -1
        try {
            log.debug(
                "sendPhotoInternal Sending message bot={} chatId={} messageType={}",
                botUsername,
                chatId,
                message.javaClass,
            )
            val bot = applicationContext.getBean<OperatorBot>(botUsername)
            return bot.execute(message)
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is TelegramApiException) {
                if (cause.message?.contains("403") == true && e.message?.contains("bot was blocked by the user") == true) {
                    log.debug("User blocked the bot: bot={} chatId={}", botUsername, chatId)
                    if (chatId > 0) {
                        usersService.disableChat(botUsername, chatId)
                    }
                }
            }
            log.debug(
                "Could not send photo to bot={} exceptionClass={} exceptionMessage=\"{}\", messageToSend={}",
                botUsername,
                e.javaClass,
                e.message,
                message,
            )
        }
        return null
    }

    private fun <T : Serializable> extractChatIdFromBotApiMethod(message: BotApiMethod<T>): Long {
        val chatId =
            when (message) {
                is SendMessage -> message.chatId
                is SendLocation -> message.chatId
                else -> null
            }
        return chatId?.toLong() ?: -1
    }

    private fun prepareText(
        text: String,
        locale: String?,
        messageArgs: Map<String, Any?>,
    ) = messageTextProcessor.prepareText(text, locale, messageArgs)

    private fun getLocale(
        botUsername: String,
        update: BotUpdate,
    ): String {
        val userChat =
            usersService.getUserChat(botUsername, ChatUtils.extractChatId(update))
                ?: return MessageTextProcessor.DEFAULT_LOCALE
        return userChat.user.locale
    }

    private fun Message.toBotMessage() =
        BotMessage(
            messageId = messageId.toString(),
            chatId = chatId,
            files = photo.orEmpty().map { BotFileReference(it.fileId, it.fileUniqueId) },
        )
}
