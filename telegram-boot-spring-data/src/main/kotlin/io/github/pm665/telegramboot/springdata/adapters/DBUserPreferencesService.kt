package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.domain.UserPreference
import io.github.pm665.telegramboot.ports.UserPreferencesService
import io.github.pm665.telegramboot.springdata.ports.UserPreferencesRepository
import org.springframework.transaction.annotation.Transactional
import io.github.pm665.telegramboot.springdata.domain.UserPreference as DbUserPreference

open class DBUserPreferencesService(
    private val userPreferencesRepository: UserPreferencesRepository,
) : UserPreferencesService {
    override fun getUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ): UserPreference? =
        userPreferencesRepository
            .findFirstByBotUsernameAndUserIdAndPreferenceName(botUsername, userId, preferenceName)
            ?.toCorePreference()

    override fun getUserPreferences(
        botUsername: String,
        userId: Long,
    ): List<UserPreference> =
        userPreferencesRepository
            .findAllByBotUsernameAndUserId(botUsername, userId)
            .map { preference -> preference.toCorePreference() }

    override fun getPreferencesByName(
        botUsername: String,
        preferenceName: String,
    ): List<UserPreference> =
        userPreferencesRepository
            .findAllByBotUsernameAndPreferenceName(botUsername, preferenceName)
            .map { preference -> preference.toCorePreference() }

    @Transactional
    override fun saveUserPreference(preference: UserPreference): UserPreference {
        val dbPreference =
            userPreferencesRepository
                .findFirstByBotUsernameAndUserIdAndPreferenceName(
                    preference.botUsername,
                    preference.userId,
                    preference.preferenceName,
                )?.also { it.value = preference.value } ?: DbUserPreference.fromCorePreference(preference)
        return userPreferencesRepository.save(dbPreference).toCorePreference()
    }

    @Transactional
    override fun deleteUserPreference(
        botUsername: String,
        userId: Long,
        preferenceName: String,
    ) {
        userPreferencesRepository.deleteByBotUsernameAndUserIdAndPreferenceName(botUsername, userId, preferenceName)
    }
}
