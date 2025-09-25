package com.example.telegram.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * Service bean that exposes simple Telegram bot behaviour.
 */
@Slf4j
public class TelegramBotService {

    private final TelegramBotProperties properties;

    public TelegramBotService(TelegramBotProperties properties) {
        Assert.notNull(properties, "TelegramBotProperties must not be null");
        this.properties = properties;
        logConfiguredName();
    }

    /**
     * Returns the configured bot name.
     *
     * @return configured bot name
     */
    public String getBotName() {
        return properties.getName();
    }

    /**
     * Logs the currently configured bot name.
     */
    public void logConfiguredName() {
        log.info("Telegram bot '{}' is ready to receive updates", properties.getName());
    }
}
