package io.github.pm665.telegramboot.events

import io.github.pm665.telegramboot.ports.BotsService.Bot

data class BotStateChangedEvent(
    val bot: Bot,
    val enabled: Boolean,
)
