package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.springdata.domain.UserPreference
import org.springframework.data.repository.ListCrudRepository

interface UserPreferencesRepository : ListCrudRepository<UserPreference, Long> {
    fun findFirstByBotUsernameAndUserIdAndPreferenceName(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ): UserPreference?

    fun findAllByBotUsernameAndUserId(
        botUsername: String,
        userId: Long,
    ): List<UserPreference>

    fun findAllByBotUsernameAndPreferenceName(
        botUsername: String,
        preferenceName: String,
    ): List<UserPreference>

    fun deleteByBotUsernameAndUserIdAndPreferenceName(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    )
}
