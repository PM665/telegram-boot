package io.github.pm665.telegramboot

import io.github.pm665.telegramboot.drivers.telegram.BotsFactory
import io.github.pm665.telegramboot.features.messaging.TelegramMessageCreator
import io.github.pm665.telegramboot.features.messaging.TelegramMessageSender
import io.github.pm665.telegramboot.infrastructure.BotManager
import io.github.pm665.telegramboot.infrastructure.TelegramBotApiHealthChecker
import io.github.pm665.telegramboot.infrastructure.TelegramPlatformBotTokenValidator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    BotsFactory::class,
    BotManager::class,
    TelegramBotApiHealthChecker::class,
    TelegramMessageCreator::class,
    TelegramMessageSender::class,
    TelegramPlatformBotTokenValidator::class,
)
class TelegramBootTelegramConfiguration
