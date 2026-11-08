package io.github.pm665.telegramboot.features.payments.drivers.scheduling

import io.github.pm665.telegramboot.domain.bot.BotInstance
import io.github.pm665.telegramboot.domain.telegram.BotPayment
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.BotPaymentsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class BotPaymentReminderJobTest {
    @Test
    fun `sends overdue payment notification through admin bot`() {
        val payments = mock<BotPaymentsService>()
        val events = mutableListOf<SendAdminMessageEvent>()
        whenever(payments.getBotPayment("alpha_bot")).thenReturn(BotPayment("alpha_bot", LocalDate.now()))
        val job =
            BotPaymentReminderJob(
                payments,
                mock<PermissionsService>(),
                mock<MessageSender>(),
                ApplicationEventPublisher { events += it as SendAdminMessageEvent },
            )

        job.execute(
            mock<BotInstance> {
                on { botUsername }.thenReturn("alpha_bot")
                on { isRunning }.thenReturn(true)
            },
        )

        assertThat(events).containsExactly(SendAdminMessageEvent("❗️ Payment is overdue for bot `alpha_bot`. Next payment date: ${LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.", emptyList(), null))
    }
}
