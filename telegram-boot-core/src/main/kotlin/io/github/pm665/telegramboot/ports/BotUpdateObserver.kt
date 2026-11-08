package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.bot.BotUpdate

fun interface BotUpdateObserver {
    fun observeUpdate(
        botUsername: String,
        update: BotUpdate,
    ): Boolean
}
