package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.UserPreference

interface UserPreferencesService {
    fun getUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ): UserPreference?

    fun getUserPreferences(
        botUsername: String,
        userId: Long,
    ): List<UserPreference>

    fun getPreferencesByName(
        botUsername: String,
        preferenceName: String,
    ): List<UserPreference>

    fun toggleBooleanPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ): Boolean {
        val current = getUserPreference(botUsername, userId, preferenceName)?.value?.toBoolean() ?: false
        val updated = !current
        saveUserPreference(botUsername, userId, preferenceName, updated.toString())
        return updated
    }

    fun saveUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
        value: String?,
    ): UserPreference = saveUserPreference(UserPreference(botUsername, userId, preferenceName, value))

    fun saveUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
        value: Boolean?,
    ): UserPreference = saveUserPreference(UserPreference(botUsername, userId, preferenceName, value?.toString()))

    fun saveUserPreference(preference: UserPreference): UserPreference

    fun deleteUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    )
}
