package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.springdata.domain.BotUser
import org.springframework.data.repository.ListCrudRepository
import kotlin.collections.Collection

interface UsersRepository : ListCrudRepository<BotUser, Long> {
    fun findByBotUsernameAndUserId(
        botUsername: String,
        userId: Long,
    ): BotUser?

    fun findAllByBotUsernameAndUserIdIn(
        botUsername: String,
        ids: Collection<Long>,
    ): List<BotUser>

    fun findFirstByBotUsernameAndUsernameIgnoreCase(
        botUsername: String,
        username: String,
    ): BotUser?
}
