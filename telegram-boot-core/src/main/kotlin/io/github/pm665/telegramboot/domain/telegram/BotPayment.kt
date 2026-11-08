package io.github.pm665.telegramboot.domain.telegram

import java.time.LocalDate

/**
 * Represents upcoming payment information for a bot.
 */
data class BotPayment(
    val botUsername: String,
    val nextPaymentDate: LocalDate,
)
