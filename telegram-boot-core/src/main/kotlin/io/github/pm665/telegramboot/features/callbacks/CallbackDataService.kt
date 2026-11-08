package io.github.pm665.telegramboot.features.callbacks

interface CallbackDataService {
    fun findById(id: String): CallbackData?

    fun createNew(callbackData: CallbackData)

    companion object {
        const val PREFIX: String = "sopcb|"
    }
}
