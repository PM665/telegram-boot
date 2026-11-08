package io.github.pm665.telegramboot.events

abstract class AdminBotCommand(
    val commands: List<String>,
    val name: String,
    val emoji: String = "",
    val showInStartMenu: Boolean = false,
    val replyInNewMessage: Boolean = false,
)
