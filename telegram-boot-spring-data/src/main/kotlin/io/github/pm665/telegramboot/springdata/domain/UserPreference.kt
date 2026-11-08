package io.github.pm665.telegramboot.springdata.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import io.github.pm665.telegramboot.domain.UserPreference as CoreUserPreference

@Table("user_preferences")
data class UserPreference(
    @Id
    var id: Long? = null,
    var botUsername: String = "",
    var userId: Long = 0,
    var preferenceName: String = "",
    var value: String? = null,
) {
    fun toCorePreference(): CoreUserPreference = CoreUserPreference(botUsername, userId, preferenceName, value)

    companion object {
        fun fromCorePreference(preference: CoreUserPreference): UserPreference =
            UserPreference(
                botUsername = preference.botUsername,
                userId = preference.userId,
                preferenceName = preference.preferenceName,
                value = preference.value,
            )
    }
}
