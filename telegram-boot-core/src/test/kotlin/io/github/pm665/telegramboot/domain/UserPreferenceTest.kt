package io.github.pm665.telegramboot.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserPreferenceTest {
    @Test
    fun `long value remains writable`() {
        val preference = UserPreference("bot", 1, "limit", "10")

        preference.longValue = 42

        assertThat(preference.longValue).isEqualTo(42L)
        assertThat(preference.value).isEqualTo("42")
    }
}
