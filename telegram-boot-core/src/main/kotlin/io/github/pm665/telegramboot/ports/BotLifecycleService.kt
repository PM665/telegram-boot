package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.ports.BotsService.Bot

interface BotLifecycleService {
    fun startBot(bot: Bot)

    fun stopBot(bot: Bot)
}
