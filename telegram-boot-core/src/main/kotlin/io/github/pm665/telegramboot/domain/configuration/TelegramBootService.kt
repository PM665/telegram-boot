package io.github.pm665.telegramboot.domain.configuration

import org.slf4j.LoggerFactory
import org.springframework.util.Assert

class TelegramBootService(
    private val properties: TelegramBootProperties,
) {
    init {
        Assert.notNull(properties, "TelegramBootProperties must not be null")
        logConfiguredName()
    }

    fun botName(): String = properties.name

    fun logConfiguredName() {
        logger.info("Telegram boot '{}' is ready to receive updates", properties.name)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TelegramBootService::class.java)
    }
}
