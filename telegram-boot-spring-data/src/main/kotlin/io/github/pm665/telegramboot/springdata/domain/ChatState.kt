package io.github.pm665.telegramboot.springdata.domain

import io.github.pm665.telegramboot.ports.ChatStatesService
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("chat_state_cache")
data class ChatState(
    @Id
    var chatId: Long = 0,
    @Column("bot_username")
    var botUsername: String = "",
    var command: String = "",
    var state: String = "",
) : Persistable<Long> {
    override fun getId(): Long = chatId

    override fun isNew(): Boolean = true

    fun toBackendChatState(): ChatStatesService.ChatState = ChatStatesService.ChatState(chatId, botUsername, command, state)
}
