package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.springdata.domain.ChatState
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository

interface ChatStatesRepository : ListCrudRepository<ChatState, Long> {
    fun findFirstByChatIdAndBotUsername(
        chatId: Long,
        botUsername: String,
    ): ChatState?

    fun deleteByChatIdAndBotUsername(
        chatId: Long,
        botUsername: String,
    )

    @Modifying
    @Query(
        """
            INSERT INTO chat_state_cache (chat_id, bot_username, command, state)
            VALUES (:chatId, :botUsername, :command, :state)
            ON DUPLICATE KEY UPDATE command = :command, state = :state
        """,
    )
    fun upsert(
        chatId: Long,
        botUsername: String,
        command: String,
        state: String,
    )
}
