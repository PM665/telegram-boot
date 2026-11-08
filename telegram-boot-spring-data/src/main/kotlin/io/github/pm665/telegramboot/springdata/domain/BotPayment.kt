package io.github.pm665.telegramboot.springdata.domain

import io.github.pm665.telegramboot.domain.telegram.BotPayment
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("bot_payment")
data class BotPayment(
    @Id
    var id: Long = 0,
    @Column("bot_username")
    var botUsername: String = "",
    @Column("next_payment_date")
    var nextPaymentDate: LocalDate = LocalDate.MIN,
) : Persistable<Long> {
    override fun getId(): Long = id

    override fun isNew(): Boolean = id == 0L

    fun toBackendBotPayment(): BotPayment = BotPayment(botUsername, nextPaymentDate)
}
