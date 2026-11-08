package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.events.AdminBotActionEvent
import io.github.pm665.telegramboot.events.AdminBotCommand
import io.github.pm665.telegramboot.events.AdminBotMessageSentEvent
import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.events.AdminMessageButton
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient

class AdminBotTest {
    @Test
    fun `back from a scheduled health detail restores the health report`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val navigation = AdminBotNavigation()
        val user = UsersService.User(42L, "alice", "en")
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendRichMessage::class.java)))
            .thenReturn(Message().apply { messageId = 77 })
        val bot = AdminBot(properties(), telegramClient, usersService, permissionsService, eventPublisher, Runnable::run, listOf(HealthCommand()), navigation)

        bot.onSendMessage(
            SendAdminMessageEvent(
                "Service health",
                listOf(42L),
                SendAdminMessageEvent.HEALTH_TAG,
                richHtml = "<p>Service health</p>",
            ),
        )
        bot.handle(callbackUpdate("/health alpha_bot"))
        bot.onSendMessage(SendAdminMessageEvent("Alpha details", listOf(42L), null, richHtml = "<p>Alpha details</p>"))
        bot.handle(callbackUpdate(AdminBotNavigation.BACK_CALLBACK))

        val edited = ArgumentCaptor.forClass(EditRichMessage::class.java)
        verify(telegramClient, times(2)).execute(edited.capture())
        assertThat(edited.allValues[0].richMessage.html).contains("Back")
        assertThat(edited.allValues[1].richMessage.html).contains("Service health")
    }

    @Test
    fun `back edits cached command response without publishing the command`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val navigation = AdminBotNavigation()
        val user = UsersService.User(42L, "alice", "en")
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(UsersService.Chat(42L, "admin_bot", 420L, true)))
        navigation.start(420, 77)
        navigation.track(HealthCommand(), AdminBotActionEvent(42L, 420L, null, "/health", 77))
        navigation.prepare(42L, 420L, SendAdminMessageEvent("Health", listOf(42L), null))
        navigation.track(HealthCommand(), AdminBotActionEvent(42L, 420L, null, "/health alpha", 77))
        navigation.prepare(42L, 420L, SendAdminMessageEvent("Alpha", listOf(42L), null))

        AdminBot(properties(), telegramClient, usersService, permissionsService, eventPublisher, Runnable::run, navigation = navigation)
            .handle(callbackUpdate(AdminBotNavigation.BACK_CALLBACK))

        verify(telegramClient).execute(any(EditRichMessage::class.java))
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `registers webhook at ngrok url with derived secret`() {
        val telegramClient = mock(TelegramClient::class.java)
        val bot =
            AdminBot(
                TelegramAdminProperties(
                    telegramLoginBotToken = "test-token",
                    telegramLoginBotUsername = "admin_bot",
                    botBaseUrl = "https://admin.example",
                    miniAppUrl = "https://admin.example/mini-app",
                ),
                telegramClient,
                mock(UsersService::class.java),
                mock(PermissionsService::class.java),
            )

        bot.registerWebhook("https://current.ngrok.app")

        val webhook = ArgumentCaptor.forClass(SetWebhook::class.java)
        verify(telegramClient).execute(webhook.capture())
        assertThat(webhook.value.url).isEqualTo("https://current.ngrok.app/bot/admin_bot")
        assertThat(webhook.value.secretToken).isEqualTo(AdminBot.secretFor("test-token"))
    }

    @Test
    fun `registers commands menu button for default and existing chats`() {
        val telegramClient = mock(TelegramClient::class.java)

        AdminBot(properties(), telegramClient, mock(UsersService::class.java), mock(PermissionsService::class.java))
            .registerCommandsMenu(listOf(42L, 99L))

        val menuButton = ArgumentCaptor.forClass(SetChatMenuButton::class.java)
        verify(telegramClient, org.mockito.Mockito.times(3)).execute(menuButton.capture())
        assertThat(menuButton.allValues.map { it.chatId }).containsExactly(null, "42", "99")
        menuButton.allValues.forEach { menu ->
            assertThat(menu.menuButton).isInstanceOf(MenuButtonCommands::class.java)
        }
    }

    @Test
    fun `replies hi to service admin start`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val user = UsersService.User(42L, "alice", "en")
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message())

        AdminBot(properties(), telegramClient, usersService, permissionsService).handle(startUpdate())

        verify(usersService).getOrCreateNewChat(42L, "admin_bot", 42L)
        val reply = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(reply.capture())
        assertThat(reply.value.chatId).isEqualTo("42")
        assertThat(reply.value.richMessage.html).isEqualTo("<h1>Hi</h1>")
    }

    @Test
    fun `start menu includes only marked commands with their emoji and name`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val user = UsersService.User(42L, "alice", "en")
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message())

        AdminBot(
            properties(),
            telegramClient,
            usersService,
            permissionsService,
            adminCommands =
                listOf(
                    TestCommand(listOf("/bots"), "Bots", "🤖", true),
                    TestCommand(listOf("/health"), "Health", "🩺", true),
                    TestCommand(listOf("/registrations"), "Registrations", "📝", true),
                    TestCommand(listOf("/hidden"), "Hidden", "🔒", false),
                ),
        ).handle(startUpdate())

        val reply = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(reply.capture())
        assertThat(reply.value.richMessage.html)
            .contains(
                "style=\"primary\" data=\"/bots\">🤖 Bots",
                "style=\"primary\" data=\"/health\">🩺 Health",
                "style=\"primary\" data=\"/registrations\">📝 Registrations",
            ).doesNotContain("/hidden", "Hidden", "🔒")
    }

    @Test
    fun `replies no access to non admin start`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val user = UsersService.User(42L, "alice", "en")
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)

        AdminBot(properties(), telegramClient, usersService, permissionsService).handle(startUpdate())

        verify(usersService, never()).getOrCreateNewChat(42L, "admin_bot", 42L)
        val reply = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(telegramClient).execute(reply.capture())
        assertThat(reply.value.text).isEqualTo("No access")
    }

    @Test
    fun `ignores unrelated updates`() {
        val telegramClient = mock(TelegramClient::class.java)

        AdminBot(properties(), telegramClient, mock(UsersService::class.java), mock(PermissionsService::class.java))
            .handle(startUpdate("/help"))

        verifyNoInteractions(telegramClient)
    }

    @Test
    fun `sends plain text health report with line breaks and replaces previous message`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(permissionsService.findServiceAdminUserIds()).thenReturn(listOf(42L))
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendMessage::class.java)))
            .thenReturn(
                Message().apply { messageId = 10 },
                Message().apply { messageId = 11 },
            )
        `when`(telegramClient.execute(any(DeleteMessage::class.java))).thenReturn(true)
        val bot = AdminBot(properties(), telegramClient, usersService, permissionsService)
        val healthReport = "Service health\n\n🤖 @alpha_bot 🟢\n🤖 @beta_bot 🔴"
        val event = SendAdminMessageEvent(healthReport, emptyList(), SendAdminMessageEvent.HEALTH_TAG)

        bot.onSendMessage(event)
        bot.onSendMessage(event.copy(recipientsUserIds = listOf(42L)))

        val sent = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(telegramClient, times(2)).execute(sent.capture())
        assertThat(sent.allValues.map { it.chatId }).containsOnly("420")
        assertThat(sent.allValues.map { it.text }).containsOnly(healthReport)
        val deleted = ArgumentCaptor.forClass(DeleteMessage::class.java)
        verify(telegramClient).execute(deleted.capture())
        assertThat(deleted.value.chatId).isEqualTo("420")
        assertThat(deleted.value.messageId).isEqualTo(10)
        verify(permissionsService).findServiceAdminUserIds()
        inOrder(telegramClient).apply {
            verify(telegramClient).execute(any(SendMessage::class.java))
            verify(telegramClient).execute(any(DeleteMessage::class.java))
            verify(telegramClient).execute(any(SendMessage::class.java))
        }
    }

    @Test
    fun `sends rich html admin messages`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message())

        AdminBot(properties(), telegramClient, usersService, mock(PermissionsService::class.java))
            .onSendMessage(SendAdminMessageEvent("Health", listOf(42L), null, richHtml = "<h1>Health</h1>"))

        val sent = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(sent.capture())
        assertThat(sent.value.richMessage.html).isEqualTo("<h1>Health</h1>")
    }

    @Test
    fun `sends tagged health rich html messages`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message())

        AdminBot(properties(), telegramClient, usersService, mock(PermissionsService::class.java))
            .onSendMessage(
                SendAdminMessageEvent(
                    "Service health",
                    listOf(42L),
                    SendAdminMessageEvent.HEALTH_TAG,
                    richHtml = "<p>Service health <tg-button>🔍</tg-button></p>",
                ),
            )

        val sent = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(sent.capture())
        assertThat(sent.value.richMessage.html).contains("<tg-button>🔍</tg-button>")
    }

    @Test
    fun `publishes sent message event for correlated outgoing message`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message().apply { messageId = 77 })

        AdminBot(properties(), telegramClient, usersService, mock(PermissionsService::class.java), eventPublisher)
            .onSendMessage(
                SendAdminMessageEvent(
                    message = "Checking health",
                    recipientsUserIds = listOf(42L),
                    tag = null,
                    requestId = "health-check-42",
                ),
            )

        verify(eventPublisher).publishEvent(AdminBotMessageSentEvent("health-check-42", 42L, 420L, 77))
    }

    @Test
    fun `edits callback source with rich response instead of sending another message`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))

        AdminBot(properties(), telegramClient, usersService, mock(PermissionsService::class.java))
            .onSendMessage(
                SendAdminMessageEvent(
                    message = "Registrations",
                    recipientsUserIds = listOf(42),
                    tag = null,
                    richHtml = "<h1>Registrations</h1><tg-button-row>Back</tg-button-row>",
                    editChatId = 420,
                    editMessageId = 77,
                ),
            )

        val edited = ArgumentCaptor.forClass(EditRichMessage::class.java)
        verify(telegramClient).execute(edited.capture())
        assertThat(edited.value.chatId).isEqualTo("420")
        assertThat(edited.value.messageId).isEqualTo(77)
        assertThat(edited.value.richMessage.html).contains("Back")
        verify(telegramClient, never()).execute(any(SendRichMessage::class.java))
    }

    @Test
    fun `sends rich callback buttons and publishes authorized callback action`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val user = UsersService.User(42L, "alice", "en")
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        val bot =
            AdminBot(
                properties(),
                telegramClient,
                usersService,
                permissionsService,
                eventPublisher,
                Runnable::run,
                listOf(HealthCommand()),
            )

        bot.onSendMessage(
            SendAdminMessageEvent(
                "Invalid Fleet sessions",
                listOf(42L),
                null,
                listOf(AdminMessageButton("Rotate session #11", "adminRotateSession:11")),
            ),
        )
        bot.handle(callbackUpdate("/health"))

        val sent = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(sent.capture())
        assertThat(sent.value.richMessage.html).contains("data=\"adminRotateSession:11\">Rotate session #11")
        verify(eventPublisher).publishEvent(AdminBotActionEvent(42L, 420L, null, "/health", 77))
        val callbackAnswer = ArgumentCaptor.forClass(AnswerCallbackQuery::class.java)
        verify(telegramClient).execute(callbackAnswer.capture())
        assertThat(callbackAnswer.value.callbackQueryId).isEqualTo("callback-id")
        assertThat(callbackAnswer.value.text).isEqualTo("Command received: Bot health check")
    }

    @Test
    fun `deletes source message after sending replacement`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val chat = UsersService.Chat(42L, "admin_bot", 420L, true)
        `when`(usersService.getChats("admin_bot", listOf(42L))).thenReturn(listOf(chat))
        `when`(telegramClient.execute(any(SendRichMessage::class.java))).thenReturn(Message())
        `when`(telegramClient.execute(any(DeleteMessage::class.java))).thenReturn(true)
        val bot = AdminBot(properties(), telegramClient, usersService, mock(PermissionsService::class.java))

        bot.onSendMessage(
            SendAdminMessageEvent(
                message = "Replacement",
                recipientsUserIds = listOf(42L),
                tag = null,
                buttons = listOf(AdminMessageButton("Back", "/registrations")),
                deleteMessageId = 77,
            ),
        )

        val deleted = ArgumentCaptor.forClass(DeleteMessage::class.java)
        inOrder(telegramClient).apply {
            verify(telegramClient).execute(any(SendRichMessage::class.java))
            verify(telegramClient).execute(deleted.capture())
        }
        assertThat(deleted.value.chatId).isEqualTo("420")
        assertThat(deleted.value.messageId).isEqualTo(77)
    }

    private fun properties() =
        TelegramAdminProperties(
            telegramLoginBotToken = "test-token",
            telegramLoginBotUsername = "admin_bot",
            botBaseUrl = "https://admin.example",
            miniAppUrl = "https://admin.example/mini-app",
        )

    private fun startUpdate(text: String = "/start"): Update =
        Update().apply {
            message =
                Message().apply {
                    this.text = text
                    chat = Chat(42L, "private")
                    from = User(42L, "Alice", false).apply { userName = "alice" }
                }
        }

    private fun callbackUpdate(data: String): Update =
        Update().apply {
            callbackQuery =
                CallbackQuery().apply {
                    id = "callback-id"
                    this.data = data
                    from = User(42L, "Alice", false).apply { userName = "alice" }
                    message =
                        Message().apply {
                            messageId = 77
                            chat = Chat(420L, "private")
                        }
                }
        }

    private class HealthCommand : AdminBotCommand(listOf("/health"), "Bot health check")

    private class TestCommand(
        commands: List<String>,
        name: String,
        emoji: String,
        showInStartMenu: Boolean,
    ) : AdminBotCommand(commands, name, emoji, showInStartMenu)
}
