package io.github.pm665.telegramboot.features.payments

import io.github.pm665.telegramboot.domain.telegram.BotPayment
import io.github.pm665.telegramboot.ports.BotPaymentsService
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class InMemoryBotPaymentsService : BotPaymentsService {
    private val payments = ConcurrentHashMap<String, BotPayment>()

    override fun getBotPayment(botUsername: String): BotPayment? = payments[botUsername]

    override fun addBotPayment(botPayment: BotPayment) {
        payments[botPayment.botUsername] = botPayment
    }

    override fun updateNextPaymentDate(
        botUsername: String,
        nextPaymentDate: LocalDate,
    ) {
        payments.compute(botUsername) { _, existing ->
            (existing ?: BotPayment(botUsername, nextPaymentDate)).copy(nextPaymentDate = nextPaymentDate)
        }
    }
}
