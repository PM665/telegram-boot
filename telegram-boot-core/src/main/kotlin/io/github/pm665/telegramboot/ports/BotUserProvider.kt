package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.BotUser

interface BotUserProvider {
    fun getBotUsers(): Collection<BotUser>

    fun addBotUser(botUser: BotUser)

    fun removeBotUser(botUserId: Long)
}
