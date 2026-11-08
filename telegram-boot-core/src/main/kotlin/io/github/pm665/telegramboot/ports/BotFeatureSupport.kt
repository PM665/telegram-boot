package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotFeature

fun interface BotFeatureSupport {
    fun isSupported(
        botUsername: String,
        feature: BotFeature,
    ): Boolean
}
