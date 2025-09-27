package io.github.pm665.telegramboot.bot.service

import org.slf4j.LoggerFactory
import org.springframework.util.Assert

class TelegramBotService(
    private val properties: TelegramBotProperties,
) {
    init {
        Assert.notNull(properties, "TelegramBotProperties must not be null")
        logConfiguredName()
    }

    fun getBotName(): String = properties.name

    fun logConfiguredName() {
        logger.info("Telegram bot '{}' is ready to receive updates", properties.name)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TelegramBotService::class.java)
    }
}
