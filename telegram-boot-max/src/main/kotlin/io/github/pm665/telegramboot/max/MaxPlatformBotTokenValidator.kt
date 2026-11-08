package io.github.pm665.telegramboot.max

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.PlatformBotTokenValidator
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

internal class MaxPlatformBotTokenValidator(
    private val apiClient: MaxApiClient,
) : PlatformBotTokenValidator {
    override val platform = BotPlatform.MAX

    override fun validate(
        token: String,
        botUsername: String,
    ) {
        val actualUsername =
            try {
                apiClient.botInfo(token).path("username").asText(null)
            } catch (exception: RestClientResponseException) {
                if (exception.statusCode in TOKEN_VALIDATION_STATUSES) {
                    throw IllegalArgumentException("Unable to verify MAX bot token", exception)
                }
                throw exception
            }
        require(actualUsername != null && actualUsername.equals(botUsername.removePrefix("@"), ignoreCase = true)) {
            "Token does not belong to @$botUsername"
        }
    }

    companion object {
        private val TOKEN_VALIDATION_STATUSES = setOf(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
    }
}
