package io.github.pm665.telegramboot.infrastructure

import io.github.pm665.telegramboot.drivers.telegram.OperatorBot
import io.github.pm665.telegramboot.ports.BotApiHealthChecker
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands

@Component
class TelegramBotApiHealthChecker(
    private val applicationContext: ApplicationContext,
) : BotApiHealthChecker {
    override fun checkApi(botUsername: String) {
        applicationContext.getBean(botUsername, OperatorBot::class.java).execute(GetMyCommands())
    }
}
