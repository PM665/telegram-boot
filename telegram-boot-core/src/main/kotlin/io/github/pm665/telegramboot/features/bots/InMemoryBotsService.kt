package io.github.pm665.telegramboot.features.bots

import io.github.pm665.telegramboot.ports.BotsService
import java.util.concurrent.ConcurrentHashMap

class InMemoryBotsService : BotsService {
    private val storage = ConcurrentHashMap<String, BotsService.Bot>()

    override fun findAllActiveBots(): Collection<BotsService.Bot> = storage.values.toList()

    override fun findBotByBotUsername(botUsername: String): BotsService.Bot? = storage[botUsername]

    fun register(bot: BotsService.Bot) {
        storage[bot.botUsername] = bot
    }

    fun register(
        token: String,
        botUsername: String,
    ) {
        register(BotsService.Bot(token, botUsername))
    }

    fun unregister(botUsername: String) {
        storage.remove(botUsername)
    }
}
