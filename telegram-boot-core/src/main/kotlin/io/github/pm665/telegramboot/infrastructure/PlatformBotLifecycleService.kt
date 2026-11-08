package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.events.BotStateChangedEvent
import io.github.pm665.telegramboot.ports.BotLifecycleDriver
import io.github.pm665.telegramboot.ports.BotLifecycleService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener

class PlatformBotLifecycleService(
    private val botsService: BotsService,
    private val drivers: List<BotLifecycleDriver>,
) : BotLifecycleService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    fun startActiveBots() {
        botsService.findAllActiveBots().forEach { bot ->
            try {
                startBot(bot)
            } catch (e: Exception) {
                log.warn("Unsuccessful attempt to start bot {}: {}", bot.botUsername, e.message, e)
            }
        }
    }

    @EventListener(BotStateChangedEvent::class)
    fun onBotStateChanged(event: BotStateChangedEvent) {
        if (event.enabled) startBot(event.bot) else stopBot(event.bot)
    }

    override fun startBot(bot: Bot) = driver(bot).startBot(bot)

    override fun stopBot(bot: Bot) = driver(bot).stopBot(bot)

    private fun driver(bot: Bot): BotLifecycleDriver =
        drivers.singleOrNull { it.platform == bot.platform }
            ?: throw IllegalStateException("No lifecycle driver for ${bot.platform} bot ${bot.botUsername}")
}
