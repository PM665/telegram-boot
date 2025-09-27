package io.github.pm665.telegramboot.bot.autoconfigure

import io.github.pm665.telegramboot.bot.service.TelegramBotProperties
import io.github.pm665.telegramboot.bot.service.TelegramBotService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(TelegramBotService::class)
@EnableConfigurationProperties(TelegramBotProperties::class)
class TelegramBotServiceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun telegramBotService(properties: TelegramBotProperties): TelegramBotService {
        return TelegramBotService(properties)
    }
}
