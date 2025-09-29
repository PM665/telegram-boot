package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Command
import io.github.pm665.telegramboot.ports.CommandProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryCommandProvider : CommandProvider {
    private data class CommandKey(val command: String, val botUsername: String)

    private val commands = ConcurrentHashMap<CommandKey, Command>()

    override fun getCommands(): Collection<Command> = commands.values.toList()

    override fun getForBot(botUsername: String): Collection<Command> =
        commands.values.filter { it.botUsername == botUsername }

    override fun getByCommand(commandName: String, botUsername: String): Command? =
        commands[CommandKey(commandName, botUsername)]

    override fun addCommand(command: Command) {
        val key = CommandKey(command.command, command.botUsername)
        commands[key] = command
    }

    override fun removeCommand(commandName: String, botUsername: String) {
        val key = CommandKey(commandName, botUsername)
        commands.remove(key)
    }
}
