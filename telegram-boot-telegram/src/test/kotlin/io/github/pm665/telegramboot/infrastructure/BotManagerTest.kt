package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.drivers.telegram.BotsFactory
import io.github.pm665.telegramboot.drivers.telegram.OperatorBot
import io.github.pm665.telegramboot.features.messaging.TelegramMessageSender
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class BotManagerTest {
    private val application = mock(TelegramBotsLongPollingApplication::class.java)
    private val botsFactory = mock(BotsFactory::class.java)
    private val messageSender = mock(TelegramMessageSender::class.java)
    private val manager = BotManager(application, botsFactory, messageSender)

    @Test
    fun `start bot exposes telegram registration failure`() {
        `when`(botsFactory.getClient("park_bot")).thenReturn(mock(OperatorBot::class.java))
        doThrow(TelegramApiException("invalid token")).`when`(application).registerBot(eq("token"), any())

        assertThatThrownBy { manager.startBot(Bot("token", "park_bot")) }
            .isInstanceOf(TelegramApiException::class.java)
            .hasMessage("invalid token")
    }
}
