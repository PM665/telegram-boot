package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.springdata.domain.BotChat
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository

interface ChatsRepository : ListCrudRepository<BotChat, Long> {
    fun findByBotUsernameAndChatId(
        botUsername: String,
        chatId: Long,
    ): BotChat?

    fun findAllByBotUsernameAndUserIdInAndEnabledTrue(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<BotChat>

    fun findFirstByUserIdAndBotUsernameAndEnabledTrue(
        userId: Long,
        botUsername: String,
    ): BotChat?

    @Query(
        """
            SELECT distinct bc.chat_id,
                   bc.bot_username,
                   bc.user_id,
                   bc.enabled
            FROM bot_chats bc
            LEFT OUTER JOIN bot_user_permissions bup ON bc.user_id = bup.user_id
                AND (bc.bot_username = bup.bot_username
                    OR (:accessType = 'SERVICE_ADMIN' AND bup.bot_username IS NULL))
            WHERE bc.bot_username = :botUsername
            AND (bup.bot_username = :botUsername
                OR (:accessType = 'SERVICE_ADMIN' AND bup.bot_username IS NULL))
            AND COALESCE(bup.access_type, 'USER') = :accessType
            AND bc.enabled != false
        """,
    )
    fun findAllByBotUsernameAndAccessType(
        botUsername: String,
        accessType: AccessType,
    ): Collection<BotChat>
}
