package io.github.pm665.telegramboot.features.admin

interface BroadcastMessagesService {
    fun save(broadcastMessage: BroadcastMessage): String

    fun remove(id: String): Boolean

    fun findById(id: String): BroadcastMessage?
}
