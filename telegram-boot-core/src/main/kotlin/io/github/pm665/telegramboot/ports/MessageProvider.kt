package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.Message

interface MessageProvider {
    fun getMessages(): Collection<Message>

    fun getByBotAndChat(
        botUsername: String,
        chatId: Long,
    ): Collection<Message>

    fun addMessage(message: Message)

    fun removeMessage(messageId: String)
}
