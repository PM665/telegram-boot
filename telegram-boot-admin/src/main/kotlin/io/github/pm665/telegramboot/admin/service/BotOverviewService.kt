package io.github.pm665.telegramboot.admin.service

import io.github.pm665.telegramboot.ports.BotsService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class BotOverviewService(
    private val botsService: BotsService,
) {
    data class BotView(
        val username: String,
        val enabled: Boolean,
    )

    fun listBots(): List<BotView> =
        botsService
            .findAllActiveBots()
            .map { BotView(username = it.botUsername, enabled = true) }
            .sortedBy { it.username.lowercase() }

    fun getBot(username: String): BotView {
        val bot =
            botsService
                .findAllActiveBots()
                .firstOrNull { it.botUsername == username }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bot with username $username not found")

        return BotView(username = bot.botUsername, enabled = true)
    }
}
