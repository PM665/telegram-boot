package io.github.pm665.telegramboot.admin.bot

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient

class AdminBotControllerTest {
    @Test
    fun `rejects missing secret without handling update`() {
        val telegramClient = mock(TelegramClient::class.java)
        val bot = bot(telegramClient)

        mockMvc(bot)
            .perform(post("/bot/admin_bot").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isForbidden)

        verifyNoInteractions(telegramClient)
    }

    @Test
    fun `rejects a different bot username`() {
        val telegramClient = mock(TelegramClient::class.java)
        val bot = bot(telegramClient)

        mockMvc(bot)
            .perform(webhookPost("other_bot", startUpdate()).header(AdminBotController.TELEGRAM_SECRET_HEADER, bot.secretToken))
            .andExpect(status().isNotFound)

        verifyNoInteractions(telegramClient)
    }

    @Test
    fun `sends hi for authenticated service admin start`() {
        val telegramClient = mock(TelegramClient::class.java)
        val usersService = mock(UsersService::class.java)
        val permissionsService = mock(PermissionsService::class.java)
        val user = UsersService.User(42L, "alice", "en")
        `when`(telegramClient.execute(org.mockito.ArgumentMatchers.any(SendRichMessage::class.java))).thenReturn(Message())
        `when`(usersService.getOrCreateUser("admin_bot", 42L, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)
        val bot = bot(telegramClient, usersService, permissionsService)

        mockMvc(bot)
            .perform(webhookPost("admin_bot", startUpdate()).header(AdminBotController.TELEGRAM_SECRET_HEADER, bot.secretToken))
            .andExpect(status().isOk)

        val reply = ArgumentCaptor.forClass(SendRichMessage::class.java)
        verify(telegramClient).execute(reply.capture())
        assertThat(reply.value.richMessage.html).isEqualTo("<h1>Hi</h1>")
    }

    @Test
    fun `acknowledges unrelated authenticated update without reply`() {
        val telegramClient = mock(TelegramClient::class.java)
        val bot = bot(telegramClient)

        mockMvc(bot)
            .perform(webhookPost("admin_bot", startUpdate("/help")).header(AdminBotController.TELEGRAM_SECRET_HEADER, bot.secretToken))
            .andExpect(status().isOk)

        verifyNoInteractions(telegramClient)
    }

    @Test
    fun `rejects malformed authenticated update`() {
        val telegramClient = mock(TelegramClient::class.java)
        val bot = bot(telegramClient)

        mockMvc(bot)
            .perform(
                post("/bot/admin_bot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{")
                    .header(AdminBotController.TELEGRAM_SECRET_HEADER, bot.secretToken),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(telegramClient)
    }

    private fun bot(
        telegramClient: TelegramClient,
        usersService: UsersService = mock(UsersService::class.java),
        permissionsService: PermissionsService = mock(PermissionsService::class.java),
    ) =
        AdminBot(
            TelegramAdminProperties(
                telegramLoginBotToken = "test-token",
                telegramLoginBotUsername = "admin_bot",
                botBaseUrl = "https://admin.example",
                miniAppUrl = "https://admin.example/mini-app",
            ),
            telegramClient,
            usersService,
            permissionsService,
        )

    private fun mockMvc(bot: AdminBot): MockMvc = MockMvcBuilders.standaloneSetup(AdminBotController(bot)).build()

    private fun webhookPost(
        botUsername: String,
        update: Update,
    ) =
        post("/bot/$botUsername")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ObjectMapper().writeValueAsString(update))

    private fun startUpdate(text: String = "/start"): Update =
        Update().apply {
            message =
                Message().apply {
                    this.text = text
                    chat = Chat(42L, "private")
                    from = User(42L, "Alice", false).apply { userName = "alice" }
                }
        }
}
