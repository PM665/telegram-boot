package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.github.pm665.telegramboot.ports.BotsService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class MaxWebhookControllerTest {
    @Test
    fun `validates MAX secret and dispatches update with originating bot name`() {
        val botsService = InMemoryBotsService().apply {
            register(BotsService.Bot("max-token", "max-driver", BotPlatform.MAX))
        }
        val handler = mock<BotUpdateHandler>()
        val executor = mock<TaskExecutorService>()
        doAnswer { it.getArgument<Runnable>(0).run() }.whenever(executor).executeImmediately(any())
        val controller = MaxWebhookController(botsService, MaxUpdateMapper(ObjectMapper()), handler, executor)

        val response =
            controller.receiveUpdate(
                "max-driver",
                MaxWebhookSecret.fromToken("max-token"),
                """{"update_type":"message_created","timestamp":1,"message":{"sender":{"user_id":77},"recipient":{"chat_type":"dialog"},"body":{"mid":"m-1","text":"/start"}}}""",
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(handler).handleUpdate(eq("max-driver"), any())
    }

    @Test
    fun `rejects invalid secret and Telegram bot`() {
        val botsService = InMemoryBotsService().apply {
            register(BotsService.Bot("max-token", "max-driver", BotPlatform.MAX))
            register(BotsService.Bot("tg-token", "telegram-driver", BotPlatform.TELEGRAM))
        }
        val handler = mock<BotUpdateHandler>()
        val executor = mock<TaskExecutorService>()
        val controller = MaxWebhookController(botsService, MaxUpdateMapper(ObjectMapper()), handler, executor)
        val body = """{"update_type":"message_created"}"""

        val invalidSecret = controller.receiveUpdate("max-driver", "bad-secret", body)
        val telegram = controller.receiveUpdate("telegram-driver", "secret", body)

        assertEquals(HttpStatus.FORBIDDEN, invalidSecret.statusCode)
        assertEquals(HttpStatus.NOT_FOUND, telegram.statusCode)
        verify(handler, never()).handleUpdate(any(), any<BotUpdate>())
    }
}
