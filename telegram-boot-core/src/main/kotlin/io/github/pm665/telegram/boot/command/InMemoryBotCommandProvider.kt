package io.github.pm665.telegram.boot.command

import java.util.concurrent.ConcurrentHashMap

class InMemoryBotCommandProvider(
    initialCommands: Collection<BotCommand> = emptyList(),
) : BotCommandProvider {

    private val commands = ConcurrentHashMap<String, BotCommand>().apply {
        initialCommands.forEach { put(it.name, it) }
    }

    override fun getCommands(): Collection<BotCommand> = commands.values.toList()

    override fun getCommand(name: String): BotCommand? = commands[name]

    override fun addCommand(command: BotCommand) {
        commands[command.name] = command
    }

    override fun removeCommand(name: String) {
        commands.remove(name)
    }
}
