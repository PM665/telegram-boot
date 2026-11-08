package io.github.pm665.telegramboot.springdata.domain

import io.github.pm665.telegramboot.ports.UsersService
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("bot_chats")
data class BotChat(
    @Id
    var id: Long? = null,
    var chatId: Long = 0,
    var botUsername: String? = null,
    var userId: Long = 0,
    var enabled: Boolean = true,
) {
    fun toDomain(botUserId: Long): UsersService.Chat =
        UsersService.Chat(
            userId = this.userId,
            botUsername = requireNotNull(this.botUsername) { "Bot chat username is required" },
            chatId = this.chatId,
            enabled = this.enabled,
            botUserId = botUserId,
        )
}
