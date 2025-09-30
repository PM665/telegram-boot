package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.Command

interface CommandProvider {
    fun getCommands(): Collection<Command>

    fun getForBot(botUsername: String): Collection<Command>

    fun getByCommand(
        commandName: String,
        botUsername: String,
    ): Command?

    fun addCommand(command: Command)

    fun removeCommand(
        commandName: String,
        botUsername: String,
    )
}
