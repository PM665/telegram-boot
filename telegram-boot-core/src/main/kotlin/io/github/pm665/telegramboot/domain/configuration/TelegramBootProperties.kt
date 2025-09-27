package io.github.pm665.telegramboot.domain.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram-boot")
class TelegramBootProperties {
    var name: String = "telegramBoot"
    var enabled: Boolean = true
}
