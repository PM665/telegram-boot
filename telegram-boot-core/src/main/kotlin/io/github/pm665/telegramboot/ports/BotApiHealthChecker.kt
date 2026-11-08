package io.github.pm665.telegramboot.ports

fun interface BotApiHealthChecker {
    fun checkApi(botUsername: String)
}
