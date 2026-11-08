package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.domain.bot.BotInstance
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.io.Serializable

/**
 * OperatorBot interface extracted from DefaultActionOperatorBot to allow bean wiring
 * and proxying against an abstraction.
 */
interface OperatorBot : BotInstance {
    val updatesConsumer: LongPollingUpdateConsumer

    override val isRunning: Boolean

    fun <T : Serializable, Method : BotApiMethod<T>> execute(method: Method): T

    fun execute(method: SendPhoto): Message

    // Optional hook for clients that retain the registered session.
    fun afterRegistration(botSession: BotSession) { /* no-op default */ }
}
