package io.github.pm665.telegramboot.admin.ngrok

import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import ngrok.NgrokInitializedEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "ngrok", name = ["enabled"], havingValue = "true")
class NgrokUrlEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    fun onNgrokInitialized(event: NgrokInitializedEvent) {
        val url = event.tunnels.firstOrNull { it.isHttps && !it.publicUrl.isNullOrBlank() }?.publicUrl ?: return
        applicationEventPublisher.publishEvent(NgrokUrlAvailableEvent(url))
    }
}
