package io.github.pm665.telegramboot.features.users

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryUsersServiceTest {
    @Test
    fun `same external user id remains separate for each bot`() {
        val usersService = InMemoryUsersService()

        val telegramUser = usersService.getOrCreateUser("telegram_bot", 42, "alice", "en")
        val maxUser = usersService.getOrCreateUser("max_bot", 42, "max-alice", "ru")

        assertThat(telegramUser.botUserId).isNotEqualTo(maxUser.botUserId)
        assertThat(usersService.getUser("telegram_bot", 42)).isEqualTo(telegramUser)
        assertThat(usersService.getUser("max_bot", 42)).isEqualTo(maxUser)
    }
}
