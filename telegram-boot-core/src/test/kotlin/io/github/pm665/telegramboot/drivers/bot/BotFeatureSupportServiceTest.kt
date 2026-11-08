package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.bot.BotFeature
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.infrastructure.BotFeatureSupportService
import io.github.pm665.telegramboot.ports.BotsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BotFeatureSupportServiceTest {
    @Test
    fun `gamification is available through telegram bot registry only`() {
        val bots =
            object : BotsService {
                override fun findAllActiveBots(): Collection<BotsService.Bot> = emptyList()

                override fun findBotByBotUsername(botUsername: String): BotsService.Bot? =
                    when (botUsername) {
                        "telegram_bot" -> BotsService.Bot("token", botUsername, BotPlatform.TELEGRAM)
                        "max_bot" -> BotsService.Bot("token", botUsername, BotPlatform.MAX)
                        else -> null
                    }
            }
        val service = BotFeatureSupportService(bots)

        assertThat(service.isSupported("telegram_bot", BotFeature.GAMIFICATION)).isTrue()
        assertThat(service.isSupported("max_bot", BotFeature.GAMIFICATION)).isFalse()
        assertThat(service.isSupported("missing_bot", BotFeature.GAMIFICATION)).isFalse()
        assertThat(service.isSupported("telegram_bot", BotFeature.AUTO_GRANT_SERVICE_ADMINS)).isTrue()
        assertThat(service.isSupported("max_bot", BotFeature.AUTO_GRANT_SERVICE_ADMINS)).isFalse()
    }
}
