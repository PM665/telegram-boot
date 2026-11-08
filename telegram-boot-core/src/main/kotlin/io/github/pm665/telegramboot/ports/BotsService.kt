package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotPlatform

interface BotsService {
    fun findAllActiveBots(): Collection<Bot>

    fun findBotByBotUsername(botUsername: String): Bot?

    open class Bot(
        val token: String,
        val botUsername: String,
        val platform: BotPlatform = BotPlatform.TELEGRAM,
    )
}
