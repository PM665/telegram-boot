package io.github.pm665.telegramboot.features.preferences

import io.github.pm665.telegramboot.domain.UserPreference
import io.github.pm665.telegramboot.ports.UserPreferencesService
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserPreferencesService : UserPreferencesService {
    private val preferences: MutableMap<PreferenceKey, UserPreference> = ConcurrentHashMap()

    override fun getUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ): UserPreference? = preferences[PreferenceKey(botUsername, userId, preferenceName)]

    override fun getUserPreferences(
        botUsername: String,
        userId: Long,
    ): List<UserPreference> =
        preferences.values
            .filter { preference -> preference.botUsername == botUsername && preference.userId == userId }
            .toList()

    override fun getPreferencesByName(
        botUsername: String,
        preferenceName: String,
    ): List<UserPreference> =
        preferences.values
            .filter { preference -> preference.botUsername == botUsername && preference.preferenceName == preferenceName }
            .toList()

    override fun saveUserPreference(preference: UserPreference): UserPreference {
        val key = PreferenceKey(preference.botUsername, preference.userId, preference.preferenceName)
        preferences[key] = preference
        return preference
    }

    override fun deleteUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ) {
        preferences.remove(PreferenceKey(botUsername, userId, preferenceName))
    }

    private data class PreferenceKey(
        val botUsername: String,
        val userId: Long,
        val preferenceName: String,
    )
}
