package sample

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.ports.BotsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SampleApplication {
    @Bean
    fun botsService(
        @Value("\${BOT_USERNAME}") username: String,
        @Value("\${BOT_TOKEN}") token: String,
    ): BotsService =
        object : BotsService {
            private val bot = BotsService.Bot(token, username.removePrefix("@"))

            override fun findAllActiveBots() = listOf(bot)

            override fun findBotByBotUsername(botUsername: String) = bot.takeIf { it.botUsername == botUsername }
        }
}

@BotCommand(name = "hello", accessType = AccessType.ALL, keepMessage = true)
class HelloCommand {
    @BotAction
    fun hello(context: BotActionContext): ActionResult {
        val username = context.user?.user?.username
        val greeting = username?.takeIf { it.isNotBlank() }?.let { "@${it.replace("_", "\\_")}" } ?: "there"
        return ActionResult.builder().messagesToSend(listOf(SendMessageMarkup("Hello $greeting"))).build()
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
