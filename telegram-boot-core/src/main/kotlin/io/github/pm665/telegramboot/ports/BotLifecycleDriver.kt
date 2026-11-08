package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.BotsService.Bot

interface BotLifecycleDriver {
    val platform: BotPlatform

    fun startBot(bot: Bot)

    fun stopBot(bot: Bot)
}
