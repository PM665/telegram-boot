package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.Bot

interface BotProvider {
    fun getBots(): Collection<Bot>
    fun addBot(bot: Bot)
    fun removeBot(botUsername: String)
}
