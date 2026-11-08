package io.github.pm665.telegramboot

import io.github.pm665.telegramboot.adapters.InMemoryBroadcastMessagesService
import io.github.pm665.telegramboot.drivers.bot.BotActionExecutorService
import io.github.pm665.telegramboot.drivers.bot.BotActionLocatorService
import io.github.pm665.telegramboot.drivers.bot.BotErrorEventListener
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.drivers.scheduling.RecurringJobsConfiguration
import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.features.admin.IAmBotAdminCommand
import io.github.pm665.telegramboot.features.admin.IAmSuperAdminCommand
import io.github.pm665.telegramboot.features.admin.MakeAnnouncementCommand
import io.github.pm665.telegramboot.features.experiment.MenuProcessor
import io.github.pm665.telegramboot.features.language.LanguageCommand
import io.github.pm665.telegramboot.features.messaging.LocalizationService
import io.github.pm665.telegramboot.features.messaging.MessageFormatter
import io.github.pm665.telegramboot.features.messaging.MustacheMessageTextProcessor
import io.github.pm665.telegramboot.features.payments.ConfirmPaymentCommand
import io.github.pm665.telegramboot.features.payments.drivers.scheduling.BotPaymentReminderJob
import io.github.pm665.telegramboot.infrastructure.BotFeatureSupportService
import io.github.pm665.telegramboot.infrastructure.BotsConfiguration
import io.github.pm665.telegramboot.infrastructure.PlatformBotLifecycleService
import io.github.pm665.telegramboot.infrastructure.PlatformBotTokenValidatorRouter
import io.github.pm665.telegramboot.infrastructure.PlatformMessageSenderRouter
import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.github.pm665.telegramboot.infrastructure.metrics.CommandMetricsService
import io.github.pm665.telegramboot.ports.BotAdminAutoApproval
import io.github.pm665.telegramboot.ports.BotLifecycleDriver
import io.github.pm665.telegramboot.ports.BotLifecycleService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PlatformMessageSender
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Configuration
@Import(
    InMemoryBroadcastMessagesService::class,
    RecurringJobsConfiguration::class,
    BotActionExecutorService::class,
    BotActionLocatorService::class,
    BotErrorEventListener::class,
    BotUpdateHandler::class,
    BotFeatureSupportService::class,
    PlatformBotTokenValidatorRouter::class,
    IAmBotAdminCommand::class,
    IAmSuperAdminCommand::class,
    MakeAnnouncementCommand::class,
    MenuProcessor::class,
    LanguageCommand::class,
    LocalizationService::class,
    MessageFormatter::class,
    MustacheMessageTextProcessor::class,
    ConfirmPaymentCommand::class,
    BotPaymentReminderJob::class,
    BotsConfiguration::class,
    TaskExecutorService::class,
    CommandMetricsService::class,
)
class TelegramBootConfiguration {
    @Bean
    fun adminBotNavigation() = AdminBotNavigation()

    @Bean
    @ConditionalOnMissingBean(BotAdminAutoApproval::class)
    fun noBotAdminAutoApproval(): BotAdminAutoApproval = BotAdminAutoApproval { _, _ -> false }

    @Bean
    @ConditionalOnMissingBean(BotLifecycleService::class)
    fun platformBotLifecycleService(
        botsService: BotsService,
        drivers: List<BotLifecycleDriver>,
    ): BotLifecycleService = PlatformBotLifecycleService(botsService, drivers)

    @Bean
    @Primary
    fun platformMessageSenderRouter(
        botsService: BotsService,
        senders: List<PlatformMessageSender>,
    ): MessageSender = PlatformMessageSenderRouter(botsService, senders)
}
