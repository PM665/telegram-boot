package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.BotTokenValidator
import io.github.pm665.telegramboot.ports.PlatformBotTokenValidator
import org.springframework.stereotype.Service

@Service
class PlatformBotTokenValidatorRouter(
    validators: List<PlatformBotTokenValidator>,
) : BotTokenValidator {
    private val validatorsByPlatform = validators.associateBy { it.platform }

    init {
        require(validatorsByPlatform.size == validators.size) { "Multiple token validators registered for a platform" }
    }

    override fun validate(
        platform: BotPlatform,
        token: String,
        botUsername: String,
    ) {
        val validator = validatorsByPlatform[platform] ?: error("No token validator registered for $platform")
        validator.validate(token, botUsername)
    }
}
