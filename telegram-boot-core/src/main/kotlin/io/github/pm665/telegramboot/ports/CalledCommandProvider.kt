package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.CalledCommand

interface CalledCommandProvider {
    fun getCalledCommands(): Collection<CalledCommand>

    fun getForBot(botUsername: String): Collection<CalledCommand>

    fun getForChat(
        botUsername: String,
        chatId: Long,
    ): Collection<CalledCommand>

    fun addCalledCommand(calledCommand: CalledCommand)
}
