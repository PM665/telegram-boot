package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.TelegramUrl
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.io.Serializable

open class DefaultActionOperatorBot(
    botToken: String,
    override val botUsername: String,
    updateHandler: BotUpdateHandler,
    telegramApiUrl: TelegramUrl = TelegramUrl.DEFAULT_URL,
) : OperatorBot { // implement interface
    private val log = LoggerFactory.getLogger(DefaultActionOperatorBot::class.java)

    override val updatesConsumer: LongPollingUpdateConsumer =
        TelegramBotUpdateConsumer(botUsername, TelegramUpdateMapper(), updateHandler)

    val telegramClient: TelegramClient = OkHttpTelegramClient(botToken, telegramApiUrl)

    var botSession: BotSession? = null

    // JVM getters are generated for properties `updatesConsumer` and `botUsername`.
    // Keep using the properties directly from Kotlin and let the compiler generate getters for Java interop.

    override fun afterRegistration(botSession: BotSession) { // added override
        log.info("Registered bot {} running state is: {}", botUsername, botSession.isRunning)
        this.botSession = botSession
    }

    override fun toString(): String = botUsername

    override val isRunning: Boolean
        get() = botSession?.isRunning == true

    @Retry(name = "telegram-api")
    override fun <T : Serializable, Method : BotApiMethod<T>> execute(method: Method): T {
        try {
            @Suppress("UNCHECKED_CAST")
            return telegramClient.execute(method) as T
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    @Retry(name = "telegram-api")
    override fun execute(method: SendPhoto): Message {
        try {
            return telegramClient.execute(method)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}
