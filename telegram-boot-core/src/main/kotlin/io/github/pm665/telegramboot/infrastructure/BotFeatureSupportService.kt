package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotFeature
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.BotFeatureSupport
import io.github.pm665.telegramboot.ports.BotsService
import org.springframework.stereotype.Service

@Service
class BotFeatureSupportService(
    private val botsService: BotsService,
) : BotFeatureSupport {
    override fun isSupported(
        botUsername: String,
        feature: BotFeature,
    ): Boolean {
        val platform = botsService.findBotByBotUsername(botUsername)?.platform ?: return false
        return when (feature) {
            BotFeature.GAMIFICATION -> platform == BotPlatform.TELEGRAM
            BotFeature.AUTO_GRANT_SERVICE_ADMINS -> platform == BotPlatform.TELEGRAM
        }
    }
}
