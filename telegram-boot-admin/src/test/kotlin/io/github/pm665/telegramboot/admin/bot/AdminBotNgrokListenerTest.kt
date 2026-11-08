package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AdminBotNgrokListenerTest {
    @Test
    fun `uses configured base url when embedded ngrok is disabled`() {
        val adminBot = mock(AdminBot::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val usersService = mock(UsersService::class.java)
        `when`(adminBot.botUsername).thenReturn("admin_bot")
        `when`(permissionsService.findServiceAdminUserIds()).thenReturn(emptyList())
        `when`(usersService.getChats("admin_bot", emptyList())).thenReturn(emptyList())

        AdminBotNgrokListener(
            adminBot,
            permissionsService,
            usersService,
            TelegramAdminProperties(botBaseUrl = "https://configured.ngrok.app"),
            embeddedNgrokEnabled = false,
        ).onApplicationReady()

        verify(adminBot).registerWebhook("https://configured.ngrok.app")
        verify(adminBot).registerCommandsMenu(emptyList())
    }

    @Test
    fun `updates webhook and menu buttons when ngrok url becomes available`() {
        val adminBot = mock(AdminBot::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val usersService = mock(UsersService::class.java)
        `when`(adminBot.botUsername).thenReturn("admin_bot")
        `when`(permissionsService.findServiceAdminUserIds()).thenReturn(listOf(1L, 2L))
        `when`(usersService.getChats("admin_bot", listOf(1L, 2L)))
            .thenReturn(
                listOf(
                    UsersService.Chat(1L, "admin_bot", 42L, true),
                    UsersService.Chat(2L, "admin_bot", 99L, true),
                ),
            )

        AdminBotNgrokListener(
            adminBot,
            permissionsService,
            usersService,
            TelegramAdminProperties(),
            embeddedNgrokEnabled = true,
        )
            .onNgrokUrlAvailable(NgrokUrlAvailableEvent("https://current.ngrok.app"))

        verify(adminBot).registerWebhook("https://current.ngrok.app")
        verify(adminBot).registerCommandsMenu(listOf(42L, 99L))
    }
}
