package io.github.pm665.telegramboot.features.admin

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.BotAdminAutoApproval
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher

class IAmBotAdminCommandTest {
    private val permissionsService = mock<PermissionsService>()
    private val usersService = mock<UsersService>()
    private val messageSender = mock<MessageSender>()
    private val autoApproval = mock<BotAdminAutoApproval>()
    private val botsService = mock<BotsService>()
    private val eventPublisher = mock<ApplicationEventPublisher>()
    private val command = IAmBotAdminCommand(permissionsService, usersService, messageSender, autoApproval, botsService, eventPublisher)

    @Test
    fun `service admin receives bot admin without manual approval`() {
        whenever(permissionsService.isUserServiceAdmin(USER)).thenReturn(true)

        val result = command.execute(context())

        assertThat(result.status).isEqualTo(ReplyStatus.ok)
        verify(permissionsService).setUserAccess(any(), eq(BOT_USERNAME), eq(io.github.pm665.telegramboot.domain.AccessType.BOT_ADMIN))
        verify(messageSender).sendMessage(any<SendMessageMarkup>(), eq(CHAT_ID), eq(BOT_USERNAME), any())
        verify(permissionsService, never()).saveApproveCodeForUser(any(), any(), any(), any())
        verify(messageSender, never()).sendMessages(any())
    }

    @Test
    fun `matching park contact receives bot admin despite username case and at sign`() {
        whenever(autoApproval.isParkContact(BOT_USERNAME, "Park_Admin")).thenReturn(true)

        val result = command.execute(context(username = "Park_Admin"))

        assertThat(result.status).isEqualTo(ReplyStatus.ok)
        verify(permissionsService).setUserAccess(any(), eq(BOT_USERNAME), eq(io.github.pm665.telegramboot.domain.AccessType.BOT_ADMIN))
        verify(messageSender).sendMessage(any<SendMessageMarkup>(), eq(CHAT_ID), eq(BOT_USERNAME), any())
        verify(permissionsService, never()).saveApproveCodeForUser(any(), any(), any(), any())
        verify(messageSender, never()).sendMessages(any())
    }

    @Test
    fun `nonmatching contact keeps manual approval flow`() {
        whenever(permissionsService.findBotAdmins(BOT_USERNAME)).thenReturn(listOf(APPROVER_CHAT))

        val result = command.execute(context(username = "other"))

        assertThat(result.status).isEqualTo(ReplyStatus.wait)
        verify(permissionsService).saveApproveCodeForUser(any(), eq(BOT_USERNAME), any(), any())
        verify(messageSender).sendMessages(any())
        verify(permissionsService, never()).setUserAccess(any(), any(), any())
    }

    @Test
    fun `already approved automatic admin receives existing response`() {
        whenever(permissionsService.isUserBotAdmin(USER, BOT_USERNAME)).thenReturn(true)

        val result = command.execute(context())

        assertThat(result.status).isEqualTo(ReplyStatus.ok)
        verify(messageSender).sendMessage(any<SendMessageMarkup>(), eq(USER.userId), eq(BOT_USERNAME), any())
        verify(permissionsService, never()).setUserAccess(any(), any(), any())
        verify(permissionsService, never()).saveApproveCodeForUser(any(), any(), any(), any())
    }

    @Test
    fun `MAX request goes to service admins in the admin bot when park has no bot admins`() {
        whenever(permissionsService.findServiceAdminUserIds()).thenReturn(listOf(8L))
        whenever(botsService.findBotByBotUsername(BOT_USERNAME))
            .thenReturn(BotsService.Bot("token", BOT_USERNAME, BotPlatform.MAX))

        val update =
            BotUpdate(
                message =
                    BotIncomingMessage(
                        chat = BotChatInfo(CHAT_ID, BotChatType.PRIVATE),
                        from = BotUserInfo(USER.userId, firstName = "Alex", lastName = "Driver"),
                    ),
            )
        val result = command.execute(context(update = update))

        assertThat(result.status).isEqualTo(ReplyStatus.wait)
        val event = ArgumentCaptor.forClass(SendAdminMessageEvent::class.java)
        verify(eventPublisher).publishEvent(event.capture())
        assertThat(event.value.recipientsUserIds).isEmpty()
        assertThat(event.value.richHtml).contains("https://max.ru/u/7", "https://max.ru/park_bot", "Alex Driver")
        assertThat(event.value.buttons.single().callbackData).startsWith("/ab 73 ").hasSizeLessThanOrEqualTo(64)
        verify(permissionsService).saveApproveCodeForUser(any(), eq(BOT_USERNAME), any(), any())
    }

    private fun context(
        username: String = USER.username!!,
        update: BotUpdate = BotUpdate(),
    ) =
        BotActionContext(
            BOT_USERNAME,
            PayloadData(CHAT_ID, update, "/iAmAdmin", null, false),
            UsersService.UserChat(USER.copy(username = username), UsersService.Chat(USER.userId, BOT_USERNAME, CHAT_ID, true)),
        )

    private companion object {
        const val BOT_USERNAME = "park_bot"
        const val CHAT_ID = 101L
        val USER = UsersService.User(7L, "park_admin", "en", botUserId = 73L, botUsername = BOT_USERNAME)
        val APPROVER_CHAT = UsersService.Chat(8L, BOT_USERNAME, 102L, true)
    }
}
