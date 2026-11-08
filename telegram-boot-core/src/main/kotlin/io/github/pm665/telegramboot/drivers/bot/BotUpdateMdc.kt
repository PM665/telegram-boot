package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import org.slf4j.MDC

internal object BotUpdateMdc {
    fun withUpdateContext(
        botUsername: String,
        update: BotUpdate,
        calledCommand: String?,
        callback: String?,
        action: () -> Unit,
    ) {
        val chatId = ChatUtils.extractChatId(update).toString()
        val closeables =
            buildList {
                add(MDC.putCloseable("bot", botUsername))
                add(MDC.putCloseable("botUsername", botUsername))
                add(MDC.putCloseable("chat", chatId))
                add(MDC.putCloseable("chatId", chatId))
                add(MDC.putCloseable("command", calledCommand ?: BotCommand.NO_COMMAND))
                add(MDC.putCloseable("callback", callback ?: BotCommand.NO_COMMAND))
                ChatUtils.extractUser(update)?.username?.takeIf { it.isNotBlank() }?.let { add(MDC.putCloseable("username", it)) }
            }

        try {
            action()
        } finally {
            closeables.asReversed().forEach(AutoCloseable::close)
        }
    }
}
