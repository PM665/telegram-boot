package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.CommandAndState
import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatMember
import io.github.pm665.telegramboot.domain.bot.BotChatMemberStatus
import io.github.pm665.telegramboot.domain.bot.BotChatMemberUpdate
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.BotUpdateObserver
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.micrometer.context.ContextRegistry
import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import java.util.concurrent.atomic.AtomicReference

class BotUpdateHandlerTest {
    companion object {
        init {
            ContextRegistry.getInstance()
                .registerThreadLocalAccessor(
                    Slf4jThreadLocalAccessor("bot", "botUsername", "chat", "chatId", "command", "callback", "username"),
                )
        }
    }

    @Test
    fun `handle adds bot username and user name to MDC`() {
        val chatStatesService = mock<ChatStatesService>()
        val botActionExecutorService = mock<BotActionExecutorService>()
        val mdcValues = AtomicReference<Map<String, String?>>()
        val update = messageUpdate("/start", "tg_alice")

        whenever(chatStatesService.getChatState(42L, "espresso_bot")).thenReturn(null)
        doAnswer {
            mdcValues.set(
                mapOf(
                    "bot" to MDC.get("bot"),
                    "botUsername" to MDC.get("botUsername"),
                    "username" to MDC.get("username"),
                    "chat" to MDC.get("chat"),
                    "chatId" to MDC.get("chatId"),
                    "command" to MDC.get("command"),
                    "callback" to MDC.get("callback"),
                ),
            )
            null
        }.whenever(botActionExecutorService).executeActionForCommandAndState(any<CommandAndState>(), eq("espresso_bot"), eq(update))

        handler(chatStatesService, botActionExecutorService).handleUpdate("espresso_bot", update)

        assertThat(mdcValues.get())
            .containsEntry("bot", "espresso_bot")
            .containsEntry("botUsername", "espresso_bot")
            .containsEntry("username", "tg_alice")
            .containsEntry("chat", "42")
            .containsEntry("chatId", "42")
            .containsEntry("command", "/start")
            .containsEntry("callback", "no-command")
        verify(botActionExecutorService).executeActionForCommandAndState(any<CommandAndState>(), eq("espresso_bot"), eq(update))
    }

    @Test
    fun `handle skips command execution when observer handles update`() {
        val chatStatesService = mock<ChatStatesService>()
        val botActionExecutorService = mock<BotActionExecutorService>()
        val update = messageUpdate("/start", "tg_alice")
        val observer = BotUpdateObserver { botUsername, observedUpdate -> botUsername == "espresso_bot" && observedUpdate == update }

        handler(chatStatesService, botActionExecutorService, listOf(observer)).handleUpdate("espresso_bot", update)

        verify(chatStatesService, never()).getChatState(any(), any())
        verify(botActionExecutorService, never()).executeActionForCommandAndState(any(), any(), any())
    }

    @Test
    fun `handle routes only private user interactions`() {
        val chatStatesService = mock<ChatStatesService>()
        val botActionExecutorService = mock<BotActionExecutorService>()
        val updates =
            listOf(
                messageUpdate("/start", "tg_alice", BotChatType.GROUP),
                BotUpdate(channelPost = BotIncomingMessage(chat = BotChatInfo(-100L, BotChatType.CHANNEL))),
                BotUpdate(editedChannelPost = BotIncomingMessage(chat = BotChatInfo(-100L, BotChatType.CHANNEL))),
                BotUpdate(
                    myChatMember =
                        BotChatMemberUpdate(
                            BotChatInfo(42L, BotChatType.PRIVATE),
                            newMember = BotChatMember(BotChatMemberStatus.MEMBER),
                        ),
                ),
                callbackUpdate(BotChatType.GROUP),
                callbackUpdate(null),
            )

        val handler = handler(chatStatesService, botActionExecutorService)
        updates.forEach { handler.handleUpdate("espresso_bot", it) }

        verify(chatStatesService, never()).getChatState(any(), any())
        verify(botActionExecutorService, never()).executeActionForCommandAndState(any(), any(), any())

        val privateCallback = callbackUpdate(BotChatType.PRIVATE)
        handler.handleUpdate("espresso_bot", privateCallback)

        verify(chatStatesService).getChatState(42L, "espresso_bot")
        verify(botActionExecutorService).executeActionForCommandAndState(any(), eq("espresso_bot"), eq(privateCallback))
    }

    @Test
    fun `handle resolves start deep link before executing command`() {
        val chatStatesService = mock<ChatStatesService>()
        val botActionExecutorService = mock<BotActionExecutorService>()
        val update = messageUpdate("/start L3JlZ2lzdGVyIDQy", "tg_alice")

        whenever(chatStatesService.getChatState(42L, "espresso_bot")).thenReturn(null)

        handler(chatStatesService, botActionExecutorService).handleUpdate("espresso_bot", update)

        verify(botActionExecutorService).executeActionForCommandAndState(
            eq(CommandAndState("/register")),
            eq("espresso_bot"),
            eq(update.copy(message = update.message?.copy(text = "/register 42"))),
        )
    }

    private fun handler(
        chatStatesService: ChatStatesService,
        botActionExecutorService: BotActionExecutorService,
        observers: List<BotUpdateObserver> = emptyList(),
    ) =
        BotUpdateHandler(
            chatStatesService,
            botActionExecutorService,
            mock<CallbackDataService>(),
            mock<ApplicationEventPublisher>(),
            observers,
        )

    private fun messageUpdate(
        text: String,
        username: String,
        chatType: BotChatType = BotChatType.PRIVATE,
    ) =
        BotUpdate(
            message =
                BotIncomingMessage(
                    chat = BotChatInfo(42L, chatType),
                    from = BotUserInfo(42L, username, "en", "Alice"),
                    text = text,
                ),
        )

    private fun callbackUpdate(chatType: BotChatType?) =
        BotUpdate(
            callbackQuery =
                BotCallbackQuery(
                    id = "callback-id",
                    data = "/start",
                    from = BotUserInfo(42L, "tg_alice"),
                    message = chatType?.let { BotIncomingMessage(chat = BotChatInfo(42L, it)) },
                ),
        )
}
