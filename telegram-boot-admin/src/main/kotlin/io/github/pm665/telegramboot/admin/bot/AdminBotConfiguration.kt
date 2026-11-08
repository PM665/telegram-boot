package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.events.AdminBotCommand
import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.AnnotatedTypeMetadata
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.net.URI

@Configuration(proxyBeanMethods = false)
@Conditional(AdminBotEnabledCondition::class)
@EnableConfigurationProperties(TelegramAdminProperties::class)
class AdminBotConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = ["adminWebhookTelegramClient"])
    fun adminWebhookTelegramClient(
        properties: TelegramAdminProperties,
        @Value("\${telegram-boot.admin.telegram-api-debug:false}") telegramApiDebug: Boolean,
        @Value("\${telegram-boot.admin.telegram-api-url:\${telegram-boot.telegram.api-url:https://api.telegram.org}}") apiUrl: String,
    ): TelegramClient {
        val uri = URI(apiUrl)
        require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null &&
            uri.rawPath.orEmpty().trim('/').isEmpty() && uri.query == null && uri.fragment == null &&
            (uri.port == -1 || uri.port in 1..65535)) { "telegram-boot.admin.telegram-api-url must be an HTTP(S) origin" }
        val telegramUrl = TelegramUrl(uri.scheme, uri.host, if (uri.port == -1) if (uri.scheme == "https") 443 else 80 else uri.port, false)
        val client = OkHttpClient.Builder()
        if (telegramApiDebug) client.addInterceptor(TelegramApiDebugInterceptor())
        return OkHttpTelegramClient(client.build(), properties.telegramLoginBotToken.trim(), telegramUrl)
    }

    @Bean
    fun adminBot(
        properties: TelegramAdminProperties,
        adminWebhookTelegramClient: TelegramClient,
        usersService: UsersService,
        permissionsService: PermissionsService,
        eventPublisher: ApplicationEventPublisher,
        adminCommands: List<AdminBotCommand>,
        navigation: AdminBotNavigation,
    ) =
        AdminBot(
            properties,
            adminWebhookTelegramClient,
            usersService,
            permissionsService,
            eventPublisher,
            adminCommands = adminCommands,
            navigation = navigation,
        )

    @Bean
    fun adminBotNgrokListener(
        adminBot: AdminBot,
        permissionsService: PermissionsService,
        usersService: UsersService,
        properties: TelegramAdminProperties,
        @Value("\${ngrok.enabled:false}") embeddedNgrokEnabled: Boolean,
    ) = AdminBotNgrokListener(adminBot, permissionsService, usersService, properties, embeddedNgrokEnabled)
}

class AdminBotEnabledCondition : Condition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean =
        !context.environment.getProperty("telegram-boot.admin.telegram-login-bot-token").isNullOrBlank() &&
            !context.environment.getProperty("telegram-boot.admin.telegram-login-bot-username").isNullOrBlank()
}
