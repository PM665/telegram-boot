package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.drivers.telegram.BotsFactory
import io.github.pm665.telegramboot.features.messaging.TelegramMessageSender
import io.github.pm665.telegramboot.ports.BotLifecycleDriver
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Service
class BotManager(
    private val application: TelegramBotsLongPollingApplication,
    private val botsFactory: BotsFactory,
    private val messageSender: TelegramMessageSender,
) : BotLifecycleDriver {
    private val log = LoggerFactory.getLogger(BotManager::class.java)
    override val platform = BotPlatform.TELEGRAM

    override fun startBot(bot: Bot) {
        require(bot.platform == platform) { "Cannot start ${bot.platform} bot with Telegram manager" }
        log.info("Starting bot {}", bot.botUsername)
        val botBean = botsFactory.getClient(bot.botUsername)
        val botSession = application.registerBot(bot.token, botBean.updatesConsumer)
        botBean.afterRegistration(botSession)
        messageSender.sendCommands(bot.botUsername, mapOf("start" to "start.command"))
    }

    override fun stopBot(bot: Bot) {
        require(bot.platform == platform) { "Cannot stop ${bot.platform} bot with Telegram manager" }
        try {
            log.info("Stopping bot {}", bot.botUsername)
            application.unregisterBot(bot.token)
        } catch (e: TelegramApiException) {
            log.warn("Unsuccessful attempt to stop bot {}: {}", bot.botUsername, e.message)
        }
    }
}
