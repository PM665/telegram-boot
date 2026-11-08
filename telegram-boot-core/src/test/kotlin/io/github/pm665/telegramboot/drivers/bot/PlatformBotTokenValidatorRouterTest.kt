package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.infrastructure.PlatformBotTokenValidatorRouter
import io.github.pm665.telegramboot.ports.PlatformBotTokenValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlatformBotTokenValidatorRouterTest {
    @Test
    fun `delegates token validation to matching platform adapter`() {
        val calls = mutableListOf<String>()
        val router =
            PlatformBotTokenValidatorRouter(
                listOf(
                    validator(BotPlatform.TELEGRAM, calls),
                    validator(BotPlatform.MAX, calls),
                ),
            )

        router.validate(BotPlatform.MAX, "max-token", "max_bot")

        assertThat(calls).containsExactly("MAX:max-token:max_bot")
    }

    @Test
    fun `fails when platform adapter is not installed`() {
        val router = PlatformBotTokenValidatorRouter(emptyList())

        assertThrows<IllegalStateException> { router.validate(BotPlatform.MAX, "token", "max_bot") }
    }

    private fun validator(
        provider: BotPlatform,
        calls: MutableList<String>,
    ) =
        object : PlatformBotTokenValidator {
            override val platform = provider

            override fun validate(
                token: String,
                botUsername: String,
            ) {
                calls += "$platform:$token:$botUsername"
            }
        }
}
