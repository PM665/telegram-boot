package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.ports.ChatStatesService.ChatState

data class CommandAndState(
    val command: String,
    val state: String,
    val callback: String? = null,
) {
    constructor(command: String) : this(command, BotAction.INIT_STATE, null)
    constructor(chatState: ChatState, callback: String?) : this(chatState.command, chatState.state, callback)

    companion object {
        val INVALID = CommandAndState(BotCommand.NO_COMMAND, BotAction.INIT_STATE, null)
    }
}
