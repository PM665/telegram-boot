package io.github.pm665.telegramboot.features.payments.drivers.scheduling

import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.bot.BotInstance
import io.github.pm665.telegramboot.drivers.scheduling.AbstractBotRecurringJob
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.BotPaymentsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class BotPaymentReminderJob(
    private val botPaymentsService: BotPaymentsService,
    private val permissionsService: PermissionsService,
    private val messageSender: MessageSender,
    private val eventPublisher: ApplicationEventPublisher,
) : AbstractBotRecurringJob() {
    public override fun execute(bot: BotInstance) {
        val botUsername = bot.botUsername
        val botPayment = botPaymentsService.getBotPayment(botUsername)
        if (botPayment == null) {
            log.debug("No payment information found for bot {}", botUsername)
            return
        }

        val daysUntilPayment = ChronoUnit.DAYS.between(LocalDate.now(), botPayment.nextPaymentDate)
        notifyServiceAdminsIfUnpaid(botUsername, botPayment.nextPaymentDate, daysUntilPayment)
        if (daysUntilPayment < 0 || daysUntilPayment > REMINDER_THRESHOLD_DAYS) {
            return
        }

        val botAdmins = permissionsService.findBotAdmins(botUsername)
        if (botAdmins.isEmpty()) {
            log.debug("No bot admins to notify about payment for bot {}", botUsername)
            return
        }

        val messageContext =
            MessageSender.SendMessageContext
                .builder()
                .botUsername(botUsername)
                .chatId(botAdmins.map { it.chatId })
                .messageToSend(SendMessageMarkup(MESSAGE_CODE))
                .messageArg("botUsername", botUsername)
                .messageArg("nextPaymentDate", formatDate(botPayment.nextPaymentDate))
                .messageArg("daysLeft", daysUntilPayment)
                .build()

        messageSender.sendMessages(messageContext)
    }

    override fun jobId(): String = JOB_ID

    private fun formatDate(nextPaymentDate: LocalDate): String = nextPaymentDate.format(DATE_FORMATTER)

    private fun notifyServiceAdminsIfUnpaid(
        botUsername: String,
        nextPaymentDate: LocalDate,
        daysUntilPayment: Long,
    ) {
        if (daysUntilPayment > 0) {
            return
        }

        eventPublisher.publishEvent(
            SendAdminMessageEvent(
                "❗️ Просрочена оплата для бота `$botUsername`. Дата следующей оплаты: ${formatDate(nextPaymentDate)}.",
                emptyList(),
                null,
            ),
        )
    }

    companion object {
        const val JOB_ID: String = "bot-payment-reminder"
        private const val REMINDER_THRESHOLD_DAYS: Long = 5
        private val log = LoggerFactory.getLogger(BotPaymentReminderJob::class.java)
        private const val MESSAGE_CODE = "botPayment.reminder"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
