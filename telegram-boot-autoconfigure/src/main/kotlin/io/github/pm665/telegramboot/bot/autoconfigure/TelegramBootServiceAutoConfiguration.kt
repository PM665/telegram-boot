package io.github.pm665.telegramboot.bot.autoconfigure

import io.github.pm665.telegramboot.adapters.InMemoryBotChatProvider
import io.github.pm665.telegramboot.adapters.InMemoryBotProvider
import io.github.pm665.telegramboot.adapters.InMemoryBotUserProvider
import io.github.pm665.telegramboot.adapters.InMemoryCommandProvider
import io.github.pm665.telegramboot.adapters.InMemoryMenuProvider
import io.github.pm665.telegramboot.adapters.InMemoryMessageProvider
import io.github.pm665.telegramboot.adapters.InMemoryUserRoleProvider
import io.github.pm665.telegramboot.domain.configuration.TelegramBootProperties
import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import io.github.pm665.telegramboot.ports.BotChatProvider
import io.github.pm665.telegramboot.ports.BotProvider
import io.github.pm665.telegramboot.ports.BotUserProvider
import io.github.pm665.telegramboot.ports.CommandProvider
import io.github.pm665.telegramboot.ports.MenuProvider
import io.github.pm665.telegramboot.ports.MessageProvider
import io.github.pm665.telegramboot.ports.UserRoleProvider
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
    fun botProvider(): BotProvider = InMemoryBotProvider()

    @Bean
    @ConditionalOnMissingBean
    fun botUserProvider(): BotUserProvider = InMemoryBotUserProvider()

    @Bean
    @ConditionalOnMissingBean
    fun botChatProvider(): BotChatProvider = InMemoryBotChatProvider()

    @Bean
    @ConditionalOnMissingBean
    fun commandProvider(): CommandProvider = InMemoryCommandProvider()

    @Bean
    @ConditionalOnMissingBean
    fun menuProvider(): MenuProvider = InMemoryMenuProvider()

    @Bean
    @ConditionalOnMissingBean
    fun userRoleProvider(): UserRoleProvider = InMemoryUserRoleProvider()

    @Bean
    @ConditionalOnMissingBean
    fun messageProvider(): MessageProvider = InMemoryMessageProvider()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram-boot", name = ["enabled"], havingValue = "true")
    fun telegramBootService(properties: TelegramBootProperties): TelegramBootService {
        return TelegramBootService(properties)
    }
}
