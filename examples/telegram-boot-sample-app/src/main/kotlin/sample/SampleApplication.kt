package sample

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.ports.BotsService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(SampleBotProperties::class)
class SampleApplication {
    @Bean
    fun botsService(properties: SampleBotProperties): BotsService =
        object : BotsService {
            private val bot = BotsService.Bot(properties.token, properties.username.removePrefix("@"))

            override fun findAllActiveBots() = listOf(bot)

            override fun findBotByBotUsername(botUsername: String) = bot.takeIf { it.botUsername == botUsername }
        }
}

@ConfigurationProperties("sample.bot")
data class SampleBotProperties(
    val username: String,
    val token: String,
)

@BotCommand(name = "hello", accessType = AccessType.ALL, keepMessage = true)
class HelloCommand {
    @BotAction
    fun hello(context: BotActionContext): ActionResult {
        val username = context.user?.user?.username
        val greeting = username?.takeIf { it.isNotBlank() } ?: "there"
        return ActionResult.builder().messagesToSend(listOf(SendMessageMarkup("Hello $greeting"))).build()
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
