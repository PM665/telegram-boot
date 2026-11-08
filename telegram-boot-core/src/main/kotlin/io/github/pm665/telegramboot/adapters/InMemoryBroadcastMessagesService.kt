package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.features.admin.BroadcastMessage
import io.github.pm665.telegramboot.features.admin.BroadcastMessagesService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InMemoryBroadcastMessagesService : BroadcastMessagesService {
    val messages = mutableMapOf<String, BroadcastMessage>()

    override fun save(broadcastMessage: BroadcastMessage): String {
        val id = UUID.randomUUID().toString()
        messages[id] = broadcastMessage
        return id
    }

    override fun remove(id: String) = messages.remove(id) != null

    override fun findById(id: String): BroadcastMessage? = messages[id]
}
