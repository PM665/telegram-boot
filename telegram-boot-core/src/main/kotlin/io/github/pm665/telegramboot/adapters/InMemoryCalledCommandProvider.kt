package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.CalledCommand
import io.github.pm665.telegramboot.ports.CalledCommandProvider
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryCalledCommandProvider : CalledCommandProvider {
    private val calledCommands = CopyOnWriteArrayList<CalledCommand>()

    override fun getCalledCommands(): Collection<CalledCommand> = calledCommands.toList()

    override fun getForBot(botUsername: String): Collection<CalledCommand> = calledCommands.filter { it.botUsername == botUsername }

    override fun getForChat(
        botUsername: String,
        chatId: Long,
    ): Collection<CalledCommand> =
        calledCommands.filter {
            it.botUsername == botUsername && it.chatId == chatId
        }

    override fun addCalledCommand(calledCommand: CalledCommand) {
        calledCommands.add(calledCommand)
    }
}
