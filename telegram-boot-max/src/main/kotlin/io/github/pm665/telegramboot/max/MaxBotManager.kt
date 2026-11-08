package io.github.pm665.telegramboot.max

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import io.github.pm665.telegramboot.ports.BotLifecycleDriver
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

internal class MaxBotManager(
    private val properties: MaxProperties,
    private val apiClient: MaxApiClient,
    private val botsService: BotsService,
    private val messageSender: MaxMessageSender,
) : BotLifecycleDriver {
    private val log = LoggerFactory.getLogger(javaClass)
    override val platform = BotPlatform.MAX

    override fun startBot(bot: Bot) {
        require(bot.platform == platform) { "Cannot start ${bot.platform} bot with MAX manager" }
        val baseUrl = properties.webhookBaseUrl.trimEnd('/')
        require(baseUrl.startsWith("https://")) { "telegram-boot.max.webhook-base-url must be an HTTPS URL" }

        val username = apiClient.botInfo(bot.token).path("username").asText(null)?.removePrefix("@")
        if (username != null && !username.equals(bot.botUsername.removePrefix("@"), ignoreCase = true)) {
            error("MAX token belongs to @$username, not @${bot.botUsername}")
        }
        val pathUsername = UriUtils.encodePathSegment(bot.botUsername, StandardCharsets.UTF_8)
        val webhookUrl = "$baseUrl/max/bot/$pathUsername"
        apiClient.subscribe(bot.token, webhookUrl, MaxWebhookSecret.fromToken(bot.token))
        messageSender.sendCommands(bot.botUsername, mapOf("start" to "start.command"))
        log.info("Started MAX bot {} with webhook", bot.botUsername)
    }

    override fun stopBot(bot: Bot) {
        require(bot.platform == platform) { "Cannot stop ${bot.platform} bot with MAX manager" }
        val baseUrl = properties.webhookBaseUrl.trimEnd('/')
        if (baseUrl.isBlank()) return
        val pathUsername = UriUtils.encodePathSegment(bot.botUsername, StandardCharsets.UTF_8)
        val webhookUrl = "$baseUrl/max/bot/$pathUsername"
        try {
            apiClient.unsubscribe(bot.token, webhookUrl)
        } catch (e: Exception) {
            log.warn("Could not remove MAX webhook for bot {}: {}", bot.botUsername, e.message)
        }
    }

    @EventListener
    fun onNgrokUrlAvailable(event: NgrokUrlAvailableEvent) {
        properties.webhookBaseUrl = event.url
        botsService.findAllActiveBots().filter { it.platform == platform }.forEach { bot ->
            try {
                startBot(bot)
            } catch (e: Exception) {
                log.warn("Could not register MAX webhook for bot {}: {}", bot.botUsername, e.message, e)
            }
        }
    }
}
