package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.PlatformBotTokenValidator
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import org.telegram.telegrambots.meta.api.methods.GetMe

internal class TelegramPlatformBotTokenValidator(private val telegramApiUrl: TelegramUrl) : PlatformBotTokenValidator {
    override val platform = BotPlatform.TELEGRAM

    override fun validate(
        token: String,
        botUsername: String,
    ) {
        val actualUsername = OkHttpTelegramClient(token, telegramApiUrl).execute(GetMe()).userName
        require(actualUsername.equals(botUsername.removePrefix("@"), ignoreCase = true)) {
            "Token does not belong to @$botUsername"
        }
    }
}
