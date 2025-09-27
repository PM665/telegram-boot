package io.github.pm665.telegramboot.bot.autoconfigure

import io.github.pm665.telegramboot.domain.configuration.TelegramBootService
import io.github.pm665.telegramboot.domain.configuration.TelegramBootProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(TelegramBootService::class)
@EnableConfigurationProperties(TelegramBootProperties::class)
class TelegramBootServiceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram-boot", name = ["enabled"], havingValue = "true")
    fun telegramBootService(properties: TelegramBootProperties): TelegramBootService {
        return TelegramBootService(properties)
    }
}
