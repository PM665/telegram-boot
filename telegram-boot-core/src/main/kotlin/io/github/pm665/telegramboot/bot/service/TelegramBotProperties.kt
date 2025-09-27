package io.github.pm665.telegramboot.bot.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
class TelegramBotProperties {
    var name: String = "telegramBot"
}
