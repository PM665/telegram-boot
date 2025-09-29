package io.github.pm665.telegram.boot.command

data class BotCommand(
    val name: String,
    val action: String,
    val enabled: Boolean,
)
