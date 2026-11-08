package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.domain.telegram.BotPayment
import io.github.pm665.telegramboot.ports.BotPaymentsService
import io.github.pm665.telegramboot.springdata.ports.BotPaymentsRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import io.github.pm665.telegramboot.springdata.domain.BotPayment as BotPaymentEntity

open class DBBotPaymentsService(
    private val botPaymentsRepository: BotPaymentsRepository,
) : BotPaymentsService {
    override fun getBotPayment(botUsername: String): BotPayment? =
        botPaymentsRepository.findByBotUsername(botUsername)?.toBackendBotPayment()

    @Transactional
    override fun addBotPayment(botPayment: BotPayment) {
        botPaymentsRepository.save(botPayment.toEntity())
    }

    @Transactional
    override fun updateNextPaymentDate(
        botUsername: String,
        nextPaymentDate: LocalDate,
    ) {
        botPaymentsRepository.upsert(botUsername, nextPaymentDate)
    }

    private fun BotPayment.toEntity(): BotPaymentEntity =
        BotPaymentEntity(
            botUsername = botUsername,
            nextPaymentDate = nextPaymentDate,
        )
}
