package io.github.pm665.telegramboot.app

import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.domain.telegram.Command
import io.github.pm665.telegramboot.domain.telegram.CommandType
import io.github.pm665.telegramboot.domain.telegram.Menu
import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import io.github.pm665.telegramboot.ports.BotProvider
import io.github.pm665.telegramboot.ports.CommandProvider
import io.github.pm665.telegramboot.ports.MenuProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TelegramBootApplication {
    @Bean
    fun telegramLogger(service: TelegramBootService): CommandLineRunner =
        CommandLineRunner {
            service.logConfiguredName()
        }

    @Bean
    fun sampleMenuRunner(
        botProvider: BotProvider,
        commandProvider: CommandProvider,
        menuProvider: MenuProvider,
    ): CommandLineRunner =
        CommandLineRunner {
            val coolerOfBots = createCoolerOfBots()

            coolerOfBots.forEach { botProvider.addBot(it) }

            coolerOfBots.forEach { bot ->
                val commands = createCommandsFor(bot)
                commands.forEach { commandProvider.addCommand(it) }

                val menus = createMenusFor(bot)
                menus.forEach { menuProvider.addMenu(it) }
            }

            coolerOfBots.forEach { bot ->
                val menusForBot =
                    menuProvider.getForBot(bot.botUsername)
                        .sortedWith(
                            compareBy<Menu> { it.section }
                                .thenBy { it.row }
                                .thenBy { it.order }
                                .thenBy { it.command },
                        )

                val menuText =
                    buildString {
                        appendLine("Bot @${bot.botUsername} menu:")
                        var currentSection: Int? = null
                        var currentRow: Int? = null
                        menusForBot.forEach { menu ->
                            if (currentSection != menu.section) {
                                currentSection = menu.section
                                currentRow = null
                                appendLine("  Section ${menu.section}:")
                            }
                            if (currentRow != menu.row) {
                                currentRow = menu.row
                                appendLine("    Row ${menu.row}:")
                            }
                            val parent = menu.parent?.let { " (parent: $it)" } ?: ""
                            appendLine("      • ${menu.command}$parent")
                        }
                    }

                logger.info("\n{}", menuText.trimEnd())
            }
        }

    private fun createCoolerOfBots(): List<Bot> =
        listOf(
            Bot(botUsername = "espresso_bot", botToken = "espresso-token", enabled = true),
            Bot(botUsername = "latte_bot", botToken = "latte-token", enabled = true),
        )

    private fun createCommandsFor(bot: Bot): List<Command> =
        listOf(
            Command(
                command = "/start",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.COMMAND,
                action = "start",
                label = "Start",
                description = "Start interacting with ${bot.botUsername}",
            ),
            Command(
                command = "/status",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.COMMAND,
                action = "status",
                label = "Status",
                description = "Check current status for ${bot.botUsername}",
            ),
            Command(
                command = "/help",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.INFO_LINK,
                action = "help",
                label = "Help",
                description = "Show help for ${bot.botUsername}",
            ),
            Command(
                command = "/settings",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.MENU,
                action = "settings",
                label = "Settings",
                description = "Configure ${bot.botUsername}",
            ),
            Command(
                command = "/settings_notifications",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.COMMAND,
                action = "settings.notifications",
                label = "Notifications",
                description = "Notification settings for ${bot.botUsername}",
            ),
            Command(
                command = "/settings_profile",
                botUsername = bot.botUsername,
                enabled = true,
                type = CommandType.COMMAND,
                action = "settings.profile",
                label = "Profile",
                description = "Profile settings for ${bot.botUsername}",
            ),
        )

    private fun createMenusFor(bot: Bot): List<Menu> =
        listOf(
            Menu(
                command = "/start",
                botUsername = bot.botUsername,
                enabled = true,
                parent = null,
                section = 0,
                row = 0,
                order = 0,
            ),
            Menu(
                command = "/status",
                botUsername = bot.botUsername,
                enabled = true,
                parent = null,
                section = 0,
                row = 0,
                order = 1,
            ),
            Menu(
                command = "/help",
                botUsername = bot.botUsername,
                enabled = true,
                parent = null,
                section = 0,
                row = 1,
                order = 0,
            ),
            Menu(
                command = "/settings",
                botUsername = bot.botUsername,
                enabled = true,
                parent = null,
                section = 1,
                row = 0,
                order = 0,
            ),
            Menu(
                command = "/settings_notifications",
                botUsername = bot.botUsername,
                enabled = true,
                parent = "/settings",
                section = 1,
                row = 0,
                order = 1,
            ),
            Menu(
                command = "/settings_profile",
                botUsername = bot.botUsername,
                enabled = true,
                parent = "/settings",
                section = 1,
                row = 1,
                order = 0,
            ),
        )

    companion object {
        private val logger = LoggerFactory.getLogger(TelegramBootApplication::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<TelegramBootApplication>(*args)
}
