package io.github.pm665.telegramboot.features.payments

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.Reply
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.ports.BotPaymentsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@BotCommand(
    name = ConfirmPaymentCommand.ACTION_ID,
    accessType = AccessType.SERVICE_ADMIN,
    displayName = "botPayment.confirm.command",
    row = 1501L,
)
@Component
class ConfirmPaymentCommand(
    private val botPaymentsService: BotPaymentsService,
    private val permissionsService: PermissionsService,
    private val messageSender: MessageSender,
) {
    @BotAction(
        replies = [
            Reply(
                onStatus = ReplyStatus.wait,
                transitionToState = STATE_WAITING_FOR_DATE,
            ),
        ],
    )
    fun execute(context: BotActionContext): ActionResult {
        val nextDate = botPaymentsService.getBotPayment(context.botUsername)?.nextPaymentDate ?: LocalDate.now()
        val plusMonth = nextDate.plusMonths(1).format(DATE_FORMATTER)
        val plus2Weeks = nextDate.plusWeeks(2).format(DATE_FORMATTER)
        val msg =
            SendMessageMarkup(
                text = "botPayment.enterNextPaymentDate",
                replyKeyboard = listOf(plusMonth, plus2Weeks),
            )
        return ActionResult
            .create(ReplyStatus.wait)
            .messagesToSend(listOf(msg))
            .messageArg("botUsername", context.botUsername)
            .build()
    }

    @BotAction(
        onState = STATE_WAITING_FOR_DATE,
        replies = [
            Reply(onStatus = ReplyStatus.ok),
            Reply(onStatus = ReplyStatus.invalidInput, messagesToSend = ["botPayment.invalidNextPaymentDate"]),
        ],
    )
    fun receiveNextPaymentDate(context: BotActionContext): ActionResult {
        val dateText = context.payloadData.message.trim()
        val botUsername = context.botUsername

        val nextPaymentDate =
            try {
                LocalDate.parse(dateText, DATE_FORMATTER)
            } catch (ex: DateTimeParseException) {
                log.warn("Failed to parse next payment date '{}' for bot {}", dateText, botUsername, ex)
                return ActionResult.create(ReplyStatus.invalidInput).build()
            }

        botPaymentsService.updateNextPaymentDate(botUsername, nextPaymentDate)
        notifyAdmins(context, botUsername, nextPaymentDate)

        return ActionResult.create(ReplyStatus.ok).build()
    }

    private fun notifyAdmins(
        context: BotActionContext,
        botUsername: String,
        nextPaymentDate: LocalDate,
    ) {
        val botAdmins = permissionsService.findBotAdmins(botUsername)
        val serviceAdmins = permissionsService.findServiceAdmins(botUsername)
        val recipients = (botAdmins + serviceAdmins).map { it.chatId }.toMutableSet()
        val initiatorChatId = context.user?.chat?.chatId ?: context.payloadData.chatId
        recipients += initiatorChatId
        if (recipients.isEmpty()) {
            log.debug("No admins found to notify about confirmed payment for bot {}", botUsername)
            return
        }

        messageSender.sendMessages(
            MessageSender.SendMessageContext
                .builder()
                .botUsername(botUsername)
                .chatId(recipients)
                .messageToSend(SendMessageMarkup("botPayment.confirmed"))
                .messageArg("botUsername", botUsername)
                .messageArg("nextPaymentDate", nextPaymentDate.format(DATE_FORMATTER))
                .build(),
        )
    }

    companion object {
        const val ACTION_ID: String = "confirmPayment"
        private const val STATE_WAITING_FOR_DATE: String = "awaitingNextPaymentDate"
        private val log = LoggerFactory.getLogger(ConfirmPaymentCommand::class.java)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
