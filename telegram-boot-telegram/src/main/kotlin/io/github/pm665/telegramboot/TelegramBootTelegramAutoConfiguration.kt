package io.github.pm665.telegramboot

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.TelegramUrl
import java.net.URI

@AutoConfiguration
@ConditionalOnClass(TelegramBootTelegramConfiguration::class)
@Import(TelegramBootTelegramConfiguration::class)
class TelegramBootTelegramAutoConfiguration {
    @Bean
    fun telegramApiUrl(@Value("\${telegram-boot.telegram.api-url:https://api.telegram.org}") value: String): TelegramUrl {
        val uri = URI(value)
        require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null &&
            uri.rawPath.orEmpty().trim('/').isEmpty() && uri.query == null && uri.fragment == null &&
            (uri.port == -1 || uri.port in 1..65535)) { "telegram-boot.telegram.api-url must be an HTTP(S) origin" }
        return TelegramUrl(uri.scheme, uri.host, if (uri.port == -1) if (uri.scheme == "https") 443 else 80 else uri.port, false)
    }

    @Bean
    @ConditionalOnMissingBean(TelegramBotsLongPollingApplication::class)
    fun telegramBotsApplication(): TelegramBotsLongPollingApplication = TelegramBotsLongPollingApplication()
}
