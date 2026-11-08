package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.ports.BotLifecycleDriver
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PlatformMessageSender
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlatformRoutingTest {
    @Test
    fun `routes message to sender for named bots platform`() {
        val botsService = InMemoryBotsService().apply {
            register(BotsService.Bot("tg-token", "telegram-driver", BotPlatform.TELEGRAM))
            register(BotsService.Bot("max-token", "max-driver", BotPlatform.MAX))
        }
        val telegram = mock<PlatformMessageSender>()
        val max = mock<PlatformMessageSender>()
        whenever(telegram.platform).thenReturn(BotPlatform.TELEGRAM)
        whenever(max.platform).thenReturn(BotPlatform.MAX)
        val router = PlatformMessageSenderRouter(botsService, listOf(telegram, max))

        router.sendTextMessage("reply", 701, "max-driver")
        router.sendTextMessage("reply", 702, "telegram-driver")

        verify(max).sendMessage(any(), eq(701L), eq("max-driver"), any())
        verify(telegram).sendMessage(any(), eq(702L), eq("telegram-driver"), any())
        verify(max, never()).sendMessage(any(), eq(702L), eq("telegram-driver"), any())
        verify(telegram, never()).sendMessage(any(), eq(701L), eq("max-driver"), any())
    }

    @Test
    fun `routes lifecycle operations to matching provider`() {
        val botsService = InMemoryBotsService()
        val telegram = mock<BotLifecycleDriver>()
        val max = mock<BotLifecycleDriver>()
        whenever(telegram.platform).thenReturn(BotPlatform.TELEGRAM)
        whenever(max.platform).thenReturn(BotPlatform.MAX)
        val service = PlatformBotLifecycleService(botsService, listOf(telegram, max))
        val bot = BotsService.Bot("max-token", "max-driver", BotPlatform.MAX)

        service.startBot(bot)

        verify(max).startBot(bot)
        verify(telegram, never()).startBot(any())
    }
}
