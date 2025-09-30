package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Message
import io.github.pm665.telegramboot.ports.MessageProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryMessageProvider : MessageProvider {
    private val messages = ConcurrentHashMap<String, Message>()

    override fun getMessages(): Collection<Message> = messages.values.toList()

    override fun getByBotAndChat(
        botUsername: String,
        chatId: Long,
    ): Collection<Message> = messages.values.filter { it.botUsername == botUsername && it.chatId == chatId }

    override fun addMessage(message: Message) {
        messages[message.id] = message
    }

    override fun removeMessage(messageId: String) {
        messages.remove(messageId)
    }
}
