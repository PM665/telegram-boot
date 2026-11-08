package io.github.pm665.telegramboot.domain.bot

interface BotInstance {
    val botUsername: String
    val isRunning: Boolean
}
