package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.springdata.domain.BotPayment
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import java.time.LocalDate

interface BotPaymentsRepository : ListCrudRepository<BotPayment, Long> {
    fun findByBotUsername(botUsername: String): BotPayment?

    @Modifying
    @Query(
        """
            INSERT INTO bot_payment (bot_username, next_payment_date)
            VALUES (:botUsername, :nextPaymentDate)
            ON DUPLICATE KEY UPDATE next_payment_date = :nextPaymentDate
        """,
    )
    fun upsert(
        botUsername: String,
        nextPaymentDate: LocalDate,
    )
}
