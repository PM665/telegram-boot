package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.context.ApplicationEventPublisher

class BotErrorEventListenerTest {
    @Test
    fun `sends park bot name with unhandled error through admin bot`() {
        val events = mutableListOf<SendAdminMessageEvent>()
        val listener =
            BotErrorEventListener(
                ApplicationEventPublisher { events += it as SendAdminMessageEvent },
            )

        listener.onBotError(BotErrorEventListener.BotErrorEvent("alpha_bot", BotUpdate(), IllegalStateException("secret")))

        assertThat(events).containsExactly(SendAdminMessageEvent("Необработанная ошибка в парковом боте @alpha_bot: IllegalStateException", emptyList(), null))
    }
}
