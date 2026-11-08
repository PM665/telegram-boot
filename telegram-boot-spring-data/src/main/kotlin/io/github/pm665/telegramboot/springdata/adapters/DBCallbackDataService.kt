package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.features.callbacks.CallbackData
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.springdata.domain.DBCallbackData
import io.github.pm665.telegramboot.springdata.ports.CallbackDataRepository

class DBCallbackDataService(
    val callbackDataRepository: CallbackDataRepository,
) : CallbackDataService {
    override fun findById(id: String): CallbackData? =
        callbackDataRepository
            .findById(id)
            .map { dBCallbackData -> CallbackData(dBCallbackData.id, dBCallbackData.data) }
            .orElse(null)

    override fun createNew(callbackData: CallbackData) {
        val dbEntity = DBCallbackData(id = callbackData.id, data = callbackData.data)
        callbackDataRepository.createNew(dbEntity)
    }
}
