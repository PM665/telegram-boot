package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.BotChat
import io.github.pm665.telegramboot.ports.BotChatProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryBotChatProvider : BotChatProvider {
    private val botChats = ConcurrentHashMap<Long, BotChat>()

    override fun getBotChats(): Collection<BotChat> = botChats.values.toList()

    override fun getForBot(botUsername: String): Collection<BotChat> = botChats.values.filter { it.botUsername == botUsername }

    override fun addBotChat(botChat: BotChat) {
        botChats[botChat.botChatId] = botChat
    }

    override fun removeBotChat(botChatId: Long) {
        botChats.remove(botChatId)
    }
}
