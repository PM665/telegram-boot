package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class BotsFactory(
    private val applicationContext: ConfigurableApplicationContext,
    private val botsService: BotsService,
    private val botUpdateHandler: BotUpdateHandler,
) : AdvisorRegisteringClientRegistry<OperatorBot>(
        appContext = applicationContext,
        clientClass = OperatorBot::class.java,
        beanCreator = { username ->
            val bot: Bot = botsService.findBotByBotUsername(username) ?: throw IllegalArgumentException("Bot not found: $username")
            DefaultActionOperatorBot(
                bot.token,
                bot.botUsername,
                botUpdateHandler,
            )
        },
    ) {
    override fun beanName(identifier: String): String = identifier
}
