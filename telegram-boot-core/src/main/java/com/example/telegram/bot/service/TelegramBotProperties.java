package com.example.telegram.bot.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties describing the Telegram bot.
 */
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {

    /**
     * Human readable name of the bot.
     */
    private String name = "telegramBot";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
