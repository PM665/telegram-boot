package io.github.pm665.telegramboot.domain.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("telegram-boot.telegram")
data class TelegramProperties(
    val apiUrl: String = "https://api.telegram.org",
)
