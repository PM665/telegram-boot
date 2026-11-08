package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotPlatform

interface PlatformMessageSender : MessageSender {
    val platform: BotPlatform
}
