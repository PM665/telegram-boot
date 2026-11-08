package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.DefaultBotActionContextProvider
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.features.callbacks.InMemoryCallbackDataService
import io.github.pm665.telegramboot.features.chatstates.InMemoryChatStatesService
import io.github.pm665.telegramboot.features.payments.InMemoryBotPaymentsService
import io.github.pm665.telegramboot.features.permissions.InMemoryPermissionsService
import io.github.pm665.telegramboot.features.preferences.InMemoryUserPreferencesService
import io.github.pm665.telegramboot.features.users.InMemoryUsersService
import io.github.pm665.telegramboot.ports.BotPaymentsService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UserPreferencesService
import io.github.pm665.telegramboot.ports.UsersService
import io.micrometer.context.ContextRegistry
import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.support.ContextPropagatingTaskDecorator
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
internal class BotsConfiguration {
    private val log = LoggerFactory.getLogger(BotsConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean
    fun defaultPermissionsService(
        usersService: UsersService,
        botsService: org.springframework.beans.factory.ObjectProvider<BotsService>,
    ): PermissionsService {
        log.warn("No PermissionsService bean found, using default implementation")
        return InMemoryPermissionsService(usersService, botsService.ifAvailable)
    }

    @Bean
    @ConditionalOnMissingBean
    fun defaultUsersService(): UsersService {
        log.warn("No UsersService bean found, using default implementation")
        return InMemoryUsersService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryBotsService(): BotsService {
        log.warn("No BotsService bean found, using default implementation")
        return InMemoryBotsService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryBotPaymentsService(): BotPaymentsService {
        log.warn("No BotPaymentsService bean found, using default implementation")
        return InMemoryBotPaymentsService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryCallbackDataService(): CallbackDataService {
        log.warn("No CallbackDataService bean found, using default implementation")
        return InMemoryCallbackDataService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryChatStatesService(): ChatStatesService {
        log.warn("No ChatStatesService bean found, using default implementation")
        return InMemoryChatStatesService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryUserPreferencesService(): UserPreferencesService {
        log.warn("No UserPreferencesService bean found, using default implementation")
        return InMemoryUserPreferencesService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun telegramBootTaskScheduler(): TaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = Runtime.getRuntime().availableProcessors()
        scheduler.setThreadNamePrefix("telegram-boot-task-")
        scheduler.setTaskDecorator(ContextPropagatingTaskDecorator())
        scheduler.initialize()
        return scheduler
    }

    @Bean
    @ConditionalOnMissingBean(name = ["telegramBootTaskExecutor"])
    fun telegramBootTaskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor("telegram-boot-async-").apply {
            setVirtualThreads(true)
            setTaskDecorator(ContextPropagatingTaskDecorator())
        }

    @Bean
    @ConditionalOnMissingBean
    fun simpleMeterRegistry(): MeterRegistry = SimpleMeterRegistry()

    @Bean
    @ConditionalOnMissingBean
    fun slf4jThreadLocalAccessor(): Slf4jThreadLocalAccessor =
        Slf4jThreadLocalAccessor("bot", "botUsername", "chat", "chatId", "command", "callback", "username")
            .also { ContextRegistry.getInstance().registerThreadLocalAccessor(it) }

    @Bean
    fun defaultBotActionContextProvider(usersService: UsersService): DefaultBotActionContextProvider =
        DefaultBotActionContextProvider(usersService)
}
