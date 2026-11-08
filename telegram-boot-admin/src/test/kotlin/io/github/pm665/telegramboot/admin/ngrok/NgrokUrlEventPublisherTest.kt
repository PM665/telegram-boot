package io.github.pm665.telegramboot.admin.ngrok

import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import ngrok.NgrokInitializedEvent
import ngrok.api.model.NgrokTunnel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

class NgrokUrlEventPublisherTest {
    @Test
    fun `publishes https ngrok url as core event`() {
        val applicationEventPublisher = mock(ApplicationEventPublisher::class.java)

        NgrokUrlEventPublisher(applicationEventPublisher)
            .onNgrokInitialized(
                NgrokInitializedEvent(
                    this,
                    listOf(
                        NgrokTunnel.builder().proto("http").publicUrl("http://current.ngrok.app").build(),
                        NgrokTunnel.builder().proto("https").publicUrl("https://current.ngrok.app").build(),
                    ),
                ),
            )

        val event = ArgumentCaptor.forClass(NgrokUrlAvailableEvent::class.java)
        verify(applicationEventPublisher).publishEvent(event.capture())
        assertThat(event.value.url).isEqualTo("https://current.ngrok.app")
    }
}
