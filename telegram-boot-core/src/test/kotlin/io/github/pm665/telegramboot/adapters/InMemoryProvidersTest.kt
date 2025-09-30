package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.domain.telegram.BotChat
import io.github.pm665.telegramboot.domain.telegram.BotUser
import io.github.pm665.telegramboot.domain.telegram.CalledCommand
import io.github.pm665.telegramboot.domain.telegram.Command
import io.github.pm665.telegramboot.domain.telegram.CommandType
import io.github.pm665.telegramboot.domain.telegram.Menu
import io.github.pm665.telegramboot.domain.telegram.Message
import io.github.pm665.telegramboot.domain.telegram.Role
import io.github.pm665.telegramboot.domain.telegram.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InMemoryProvidersTest {
    @Test
    fun `bot provider replaces bot with same username`() {
        val provider = InMemoryBotProvider()
        val initial = Bot(botUsername = "espresso_bot", botToken = "old", enabled = true)
        val replacement = initial.copy(botToken = "new", enabled = false)

        provider.addBot(initial)
        provider.addBot(replacement)

        val stored = provider.getBots().single { it.botUsername == "espresso_bot" }
        assertThat(stored.botToken).isEqualTo("new")
        assertThat(stored.enabled).isFalse()

        provider.removeBot("espresso_bot")
        assertThat(provider.getBots()).isEmpty()
    }

    @Test
    fun `bot user provider stores and removes users`() {
        val provider = InMemoryBotUserProvider()
        val user = BotUser(botUserId = 1L, username = "barista", active = true)

        provider.addBotUser(user)
        assertThat(provider.getBotUsers()).containsExactly(user)

        provider.removeBotUser(1L)
        assertThat(provider.getBotUsers()).isEmpty()
    }

    @Test
    fun `bot chat provider filters by bot and removes chats`() {
        val provider = InMemoryBotChatProvider()
        val chat = BotChat(botUsername = "espresso_bot", botUserId = 1L, botChatId = 42L)
        val other = BotChat(botUsername = "latte_bot", botUserId = 2L, botChatId = 100L)

        provider.addBotChat(chat)
        provider.addBotChat(other)

        assertThat(provider.getForBot("espresso_bot")).containsExactly(chat)

        provider.removeBotChat(42L)
        assertThat(provider.getBotChats()).containsExactly(other)
    }

    @Test
    fun `command provider retrieves and replaces commands`() {
        val provider = InMemoryCommandProvider()
        val command =
            Command(
                command = "/start",
                botUsername = "espresso_bot",
                enabled = true,
                type = CommandType.COMMAND,
                action = "start",
                label = "Start",
                description = "Start",
            )
        provider.addCommand(command)

        val replacement = command.copy(description = "Start again")
        provider.addCommand(replacement)

        assertThat(provider.getByCommand("/start", "espresso_bot")).isEqualTo(replacement)
        assertThat(provider.getForBot("espresso_bot")).containsExactly(replacement)

        provider.removeCommand("/start", "espresso_bot")
        assertThat(provider.getCommands()).isEmpty()
    }

    @Test
    fun `called command provider filters by bot and chat`() {
        val provider = InMemoryCalledCommandProvider()
        val timestamp = LocalDateTime.now()
        val target =
            CalledCommand(
                timestamp = timestamp,
                botUsername = "espresso_bot",
                chatId = 99L,
                commandName = "/start",
                messageId = 1L,
                result = true,
                outcome = "ok",
            )
        val other =
            target.copy(
                botUsername = "latte_bot",
                chatId = 100L,
                messageId = 2L,
            )

        provider.addCalledCommand(target)
        provider.addCalledCommand(other)

        assertThat(provider.getForBot("espresso_bot")).containsExactly(target)
        assertThat(provider.getForChat("espresso_bot", 99L)).containsExactly(target)
    }

    @Test
    fun `menu provider returns menus by bot and parent`() {
        val provider = InMemoryMenuProvider()
        val root = Menu(command = "/root", botUsername = "espresso_bot", enabled = true, parent = null, section = 0, row = 0, order = 0)
        val child = root.copy(command = "/child", parent = "/root", order = 1)
        val otherBot = root.copy(botUsername = "latte_bot", command = "/latte")

        provider.addMenu(root)
        provider.addMenu(child)
        provider.addMenu(otherBot)

        assertThat(provider.getForBot("espresso_bot")).containsExactlyInAnyOrder(root, child)
        assertThat(provider.getByParent("/root", "espresso_bot")).containsExactly(child)

        provider.removeMenu("/root", "espresso_bot")
        assertThat(provider.getForBot("espresso_bot")).containsExactly(child)
    }

    @Test
    fun `message provider filters by bot and chat`() {
        val provider = InMemoryMessageProvider()
        val message = Message(id = "1", botUsername = "espresso_bot", chatId = 42L, className = "Update", update = Any())
        val other = Message(id = "2", botUsername = "espresso_bot", chatId = 100L, className = "Update", update = Any())

        provider.addMessage(message)
        provider.addMessage(other)

        assertThat(provider.getByBotAndChat("espresso_bot", 42L)).containsExactly(message)

        provider.removeMessage("1")
        assertThat(provider.getMessages()).containsExactly(other)
    }

    @Test
    fun `user role provider treats bot username as part of key`() {
        val provider = InMemoryUserRoleProvider()
        val globalAdmin = UserRole(botUsername = null, botUserId = 1L, role = Role.SUPER_ADMIN)
        val scopedAdmin = UserRole(botUsername = "espresso_bot", botUserId = 1L, role = Role.BOT_ADMIN)

        provider.addUserRole(globalAdmin)
        provider.addUserRole(scopedAdmin)

        assertThat(provider.getUserRoles()).containsExactlyInAnyOrder(globalAdmin, scopedAdmin)
        assertThat(provider.getForBot("espresso_bot")).containsExactly(scopedAdmin)

        provider.removeUserRole(null, 1L, Role.SUPER_ADMIN)
        assertThat(provider.getUserRoles()).containsExactly(scopedAdmin)
    }
}
