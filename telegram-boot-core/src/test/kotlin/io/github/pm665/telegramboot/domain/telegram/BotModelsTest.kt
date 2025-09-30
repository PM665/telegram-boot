package io.github.pm665.telegramboot.domain.telegram

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BotModelsTest {
    @Test
    fun `bot user defaults to inactive`() {
        val botUser = BotUser(botUserId = 1L, username = "coffee")

        assertThat(botUser.active).isFalse()
    }

    @Test
    fun `menu equality is based on structural data`() {
        val menuOne =
            Menu(
                command = "/brew",
                botUsername = "espresso_bot",
                enabled = true,
                parent = null,
                section = 0,
                row = 0,
                order = 0,
            )
        val menuTwo = menuOne.copy()

        assertThat(menuOne).isEqualTo(menuTwo)
        assertThat(menuOne.hashCode()).isEqualTo(menuTwo.hashCode())
    }
}
