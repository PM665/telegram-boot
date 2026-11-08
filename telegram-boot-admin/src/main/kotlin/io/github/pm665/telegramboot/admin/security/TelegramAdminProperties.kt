package io.github.pm665.telegramboot.admin.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram-boot.admin")
data class TelegramAdminProperties(
    val telegramLoginBotToken: String = "",
    val telegramLoginBotUsername: String = "",
    val telegramLoginMaxAgeSeconds: Long = 300,
    val botBaseUrl: String = "",
    val miniAppUrl: String = "",
)
