package io.github.pm665.telegram.boot.command

interface BotCommandProvider {
    fun getCommands(): Collection<BotCommand>

    fun getCommand(name: String): BotCommand?

    fun addCommand(command: BotCommand)

    fun removeCommand(name: String)
}
