package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.BotChat

interface BotChatProvider {
    fun getBotChats(): Collection<BotChat>

    fun getForBot(botUsername: String): Collection<BotChat>

    fun addBotChat(botChat: BotChat)

    fun removeBotChat(botChatId: Long)
}
