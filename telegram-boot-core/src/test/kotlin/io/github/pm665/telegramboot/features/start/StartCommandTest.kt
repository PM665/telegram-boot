package io.github.pm665.telegramboot.features.start

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.bot.BotFeature
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.BotCommandsProvider
import io.github.pm665.telegramboot.ports.BotFeatureSupport
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class StartCommandTest {
    @Test
    fun `menu shows only visible supported commands allowed for user in order`() {
        val user = UsersService.User(1, "alice", "en")
        val userChat = UsersService.UserChat(user, UsersService.Chat(1, "bot", 1, true))
        val context = BotActionContext("bot", ChatUtils.createPayloadData(BotUpdate()), userChat)
        val allowed = BotCommand(name = "allowed", displayName = "Allowed", accessType = AccessType.ALL, row = 2)
        val first = BotCommand(name = "first", accessType = AccessType.ALL, row = 1)
        val hidden = BotCommand(name = "hidden", visibility = Visibility.HIDDEN)
        val denied = BotCommand(name = "denied", accessType = AccessType.SERVICE_ADMIN)
        val unsupported = BotCommand(name = "game", features = arrayOf(BotFeature.GAMIFICATION))
        val permissions = mock(PermissionsService::class.java)
        listOf(allowed, first, unsupported).forEach {
            `when`(permissions.hasUserAccessToCommand("bot", userChat, it)).thenReturn(true)
        }
        val command = StartCommand(
            object : BotCommandsProvider {
                override fun findAllBotCommands() = listOf(allowed, hidden, denied, unsupported, first)
            },
            permissions,
            BotFeatureSupport { _, _ -> false },
        )

        val menu = requireNotNull(command.start(context).messagesToSend.single().menuMarkup)
        assertThat(menu.rich).isTrue()
        assertThat(menu.rows.map { row -> row.buttons.map { it.text to it.callbackData } })
            .containsExactly(listOf("first" to "/first"), listOf("Allowed" to "/allowed"))
    }
}
