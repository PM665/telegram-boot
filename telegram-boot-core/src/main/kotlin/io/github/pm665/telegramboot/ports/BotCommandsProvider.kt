package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.annotations.BotCommand

interface BotCommandsProvider {
    fun findAllBotCommands(): List<BotCommand>
}
