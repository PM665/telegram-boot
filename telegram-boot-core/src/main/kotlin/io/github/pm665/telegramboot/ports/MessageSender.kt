package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.actions.SendLocationItem
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.domain.bot.BotUpdate

interface MessageSender {
    fun sendMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
        messageArgs: Map<String, String>,
    ): BotMessage?

    fun sendLocation(
        lat: Double,
        lon: Double,
        chatId: Long,
        botUsername: String,
    ): BotMessage?

    fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String> = emptyMap(),
        botUsername: String,
    )

    fun sendCallbackNotification(
        update: BotUpdate,
        text: String,
        messageArgs: Map<String, String> = emptyMap(),
        botUsername: String,
        showAlert: Boolean,
    ) = sendCallbackNotification(update, text, messageArgs, botUsername)

    fun deleteCallbackSource(
        update: BotUpdate?,
        botUsername: String,
    )

    fun deleteMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    )

    fun pinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    )

    fun unpinMessage(
        messageId: String?,
        chatId: Long,
        botUsername: String,
    )

    fun sendMessages(context: SendMessageContext): Collection<BotMessage>

    fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
    )

    fun editMessage(
        botUsername: String,
        update: BotUpdate,
        text: String,
        inlineKeyboard: MenuMarkup?,
        messageArgs: Map<String, Any>,
        parseMode: String?,
    ) = editMessage(botUsername, update, text, inlineKeyboard, messageArgs)

    fun sendCommands(
        botUsername: String,
        commands: Map<String, String>,
    )

    fun sendMessage(
        sendMessageMarkup: SendMessageMarkup,
        chatId: Long,
        botUsername: String,
    ): BotMessage? = sendMessage(sendMessageMarkup, chatId, botUsername, emptyMap())

    fun sendTextMessage(
        text: String,
        chatId: Long,
        botUsername: String,
    ): BotMessage? = sendMessage(SendMessageMarkup(text), chatId, botUsername)

    data class SendMessageContext(
        val actionId: String?,
        val messagesToSend: List<SendMessageMarkup>,
        val messageArgs: Map<String, Any?>,
        val locationsToSend: List<SendLocationItem>,
        val chatId: Set<Long>,
        val botUsername: String?,
        val pin: Boolean,
        val silent: Boolean,
    ) {
        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder internal constructor() {
            private var actionId: String? = null
            private val messagesToSend = mutableListOf<SendMessageMarkup>()
            private val messageArgs = linkedMapOf<String, Any?>()
            private val locationsToSend = mutableListOf<SendLocationItem>()
            private val chatIds = linkedSetOf<Long>()
            private var botUsername: String? = null
            private var pin: Boolean = false
            private var silent: Boolean = false

            fun actionId(actionId: String) = apply { this.actionId = actionId }

            fun messageToSend(message: SendMessageMarkup) = apply { messagesToSend += message }

            fun messagesToSend(messages: Collection<SendMessageMarkup>) = apply { messagesToSend += messages }

            fun messageArg(
                key: String,
                value: Any?,
            ) = apply { messageArgs[key] = value }

            fun messageArgs(args: Map<String, *>) = apply { messageArgs.putAll(args) }

            fun locationsToSend(locations: Collection<SendLocationItem>) = apply { locationsToSend += locations }

            fun chat(chatId: Long) = apply { chatIds += chatId }

            fun chatId(chatIds: Collection<Long>) = apply { this.chatIds += chatIds }

            fun botUsername(botUsername: String) = apply { this.botUsername = botUsername }

            fun pin(pin: Boolean) = apply { this.pin = pin }

            fun silent(silent: Boolean) = apply { this.silent = silent }

            fun build(): SendMessageContext =
                SendMessageContext(
                    actionId = actionId,
                    messagesToSend = messagesToSend.toList(),
                    messageArgs = HashMap(messageArgs),
                    locationsToSend = locationsToSend.toList(),
                    chatId = chatIds.toSet(),
                    botUsername = botUsername,
                    pin = pin,
                    silent = silent,
                )
        }
    }
}
