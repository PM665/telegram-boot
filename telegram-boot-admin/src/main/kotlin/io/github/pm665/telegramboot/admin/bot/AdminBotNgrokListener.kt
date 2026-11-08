package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

class AdminBotNgrokListener(
    private val adminBot: AdminBot,
    private val permissionsService: PermissionsService,
    private val usersService: UsersService,
    private val properties: TelegramAdminProperties,
    private val embeddedNgrokEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onNgrokUrlAvailable(event: NgrokUrlAvailableEvent) {
        register(event.url)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (!embeddedNgrokEnabled) {
            register(properties.botBaseUrl)
        }
    }

    private fun register(configuredBaseUrl: String) {
        val botBaseUrl = configuredBaseUrl.trimEnd('/')
        log.info("Registering admin bot {} at {}", adminBot.botUsername, botBaseUrl)
        adminBot.registerWebhook(botBaseUrl)
        adminBot.registerCommandsMenu(
            usersService
                .getChats(adminBot.botUsername, permissionsService.findServiceAdminUserIds())
                .map { it.chatId },
        )
    }
}
