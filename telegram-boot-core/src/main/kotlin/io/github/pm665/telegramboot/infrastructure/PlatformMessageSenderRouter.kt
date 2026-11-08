package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PlatformMessageSender

class PlatformMessageSenderRouter(
    private val botsService: BotsService,
    private val senders: List<PlatformMessageSender>,
) : MessageSender {
    override fun sendMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
        messageArgs: Map<String, String>,
    ) = sender(botUsername).sendMessage(sendMessageMarkup, chatId, botUsername, messageArgs)

    override fun sendLocation(
        lat: Double,
        lon: Double,
        chatId: Long,
        botUsername: String,
    ) = sender(botUsername).sendLocation(lat, lon, chatId, botUsername)

    override fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String>,
        botUsername: String,
    ) = sender(botUsername).sendCallbackNotification(update, text, messageArgs, botUsername)

    override fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String>,
        botUsername: String,
        showAlert: Boolean,
    ) = sender(botUsername).sendCallbackNotification(update, text, messageArgs, botUsername, showAlert)

    override fun deleteCallbackSource(
        update: BotUpdate?,
        botUsername: String,
    ) = sender(botUsername).deleteCallbackSource(update, botUsername)

    override fun deleteMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) = sender(botUsername).deleteMessage(messageId, chatId, botUsername)

    override fun pinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) = sender(botUsername).pinMessage(messageId, chatId, botUsername)

    override fun unpinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    ) = sender(botUsername).unpinMessage(messageId, chatId, botUsername)

    override fun sendMessages(context: MessageSender.SendMessageContext) =
        context.botUsername?.let { sender(it).sendMessages(context) } ?: emptyList()

    override fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
    ) = sender(botUsername).editMessage(botUsername, update, text, inlineKeyboard, messageArgs)

    override fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
        parseMode: String?,
    ) = sender(botUsername).editMessage(botUsername, update, text, inlineKeyboard, messageArgs, parseMode)

    override fun sendCommands(
        botUsername: String,
        commands: Map<String, String>,
    ) = sender(botUsername).sendCommands(botUsername, commands)

    private fun sender(botUsername: String): PlatformMessageSender {
        val platform =
            botsService.findBotByBotUsername(botUsername)?.platform
                ?: throw IllegalArgumentException("Bot not found: $botUsername")
        return senders.singleOrNull { it.platform == platform }
            ?: throw IllegalStateException("No message sender for $platform bot $botUsername")
    }
}
