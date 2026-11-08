package io.github.pm665.telegramboot

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication

@AutoConfiguration
@ConditionalOnClass(TelegramBootTelegramConfiguration::class)
@Import(TelegramBootTelegramConfiguration::class)
class TelegramBootTelegramAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(TelegramBotsLongPollingApplication::class)
    fun telegramBotsApplication(): TelegramBotsLongPollingApplication = TelegramBotsLongPollingApplication()
}
