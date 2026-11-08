package io.github.pm665.telegramboot.features.callbacks

import java.util.concurrent.ConcurrentHashMap

class InMemoryCallbackDataService : CallbackDataService {
    private val storage = ConcurrentHashMap<String, CallbackData>()

    override fun findById(id: String): CallbackData? = storage[id]

    override fun createNew(callbackData: CallbackData) {
        storage[callbackData.id] = callbackData
    }
}
