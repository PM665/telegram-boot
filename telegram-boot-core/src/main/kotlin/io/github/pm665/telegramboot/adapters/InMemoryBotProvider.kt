package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.ports.BotProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryBotProvider : BotProvider {
    private val bots = ConcurrentHashMap<String, Bot>()

    override fun getBots(): Collection<Bot> = bots.values.toList()

    override fun addBot(bot: Bot) {
        bots[bot.botUsername] = bot
    }

    override fun removeBot(botUsername: String) {
        bots.remove(botUsername)
    }
}
