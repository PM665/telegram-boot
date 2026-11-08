package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BotErrorEventListener(
    private val eventPublisher: ApplicationEventPublisher,
) {
    @EventListener(BotErrorEvent::class)
    fun onBotError(event: BotErrorEvent) {
        eventPublisher.publishEvent(
            SendAdminMessageEvent(
                "Unhandled error in fleet bot @${event.botUsername}: ${event.exception.javaClass.simpleName}",
                emptyList(),
                null,
            ),
        )
    }

    data class BotErrorEvent(
        val botUsername: String,
        val update: BotUpdate?,
        val exception: Throwable,
    )
}
