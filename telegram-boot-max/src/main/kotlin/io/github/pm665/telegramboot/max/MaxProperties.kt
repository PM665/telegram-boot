package io.github.pm665.telegramboot.max

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("telegram-boot.max")
class MaxProperties {
    var apiUrl: String = "https://platform-api2.max.ru"
    var webhookBaseUrl: String = ""
}
