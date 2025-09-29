package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.BotUser
import io.github.pm665.telegramboot.ports.BotUserProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryBotUserProvider : BotUserProvider {
    private val botUsers = ConcurrentHashMap<Long, BotUser>()

    override fun getBotUsers(): Collection<BotUser> = botUsers.values.toList()

    override fun addBotUser(botUser: BotUser) {
        botUsers[botUser.botUserId] = botUser
    }

    override fun removeBotUser(botUserId: Long) {
        botUsers.remove(botUserId)
    }
}
