package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotPlatform

fun interface BotTokenValidator {
    fun validate(
        platform: BotPlatform,
        token: String,
        botUsername: String,
    )
}

interface PlatformBotTokenValidator {
    val platform: BotPlatform

    fun validate(
        token: String,
        botUsername: String,
    )
}
