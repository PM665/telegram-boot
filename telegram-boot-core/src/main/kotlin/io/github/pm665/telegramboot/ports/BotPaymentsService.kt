package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.BotPayment
import java.time.LocalDate

interface BotPaymentsService {
    fun getBotPayment(botUsername: String): BotPayment?

    fun addBotPayment(botPayment: BotPayment)

    fun updateNextPaymentDate(
        botUsername: String,
        nextPaymentDate: LocalDate,
    )
}
