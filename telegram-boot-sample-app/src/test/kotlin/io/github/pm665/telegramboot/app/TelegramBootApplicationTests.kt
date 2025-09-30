package io.github.pm665.telegramboot.app

import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import io.github.pm665.telegramboot.ports.BotProvider
import io.github.pm665.telegramboot.ports.CommandProvider
import io.github.pm665.telegramboot.ports.MenuProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@SpringBootTest(
    classes = [TelegramBootApplication::class],
    properties = [
        "telegram-boot.enabled=true",
        "telegram-boot.name=test-bot",
    ],
)
@ExtendWith(OutputCaptureExtension::class)
class TelegramBootApplicationTests {
    @Autowired
    private lateinit var sampleMenuRunner: CommandLineRunner

    @Autowired
    private lateinit var telegramLogger: CommandLineRunner

    @Autowired
    private lateinit var botProvider: BotProvider

    @Autowired
    private lateinit var commandProvider: CommandProvider

    @Autowired
    private lateinit var menuProvider: MenuProvider

    @Autowired
    private lateinit var telegramBootService: TelegramBootService

    @Test
    fun `sample menu runner populates providers`() {
        sampleMenuRunner.run()

        val botUsernames = botProvider.getBots().map(Bot::botUsername)
        assertThat(botUsernames).containsExactlyInAnyOrder("espresso_bot", "latte_bot")

        val espressoCommands = commandProvider.getForBot("espresso_bot")
        assertThat(espressoCommands).hasSizeGreaterThanOrEqualTo(1)
        assertThat(espressoCommands.map { it.command })
            .contains("/settings_notifications", "/settings_profile")

        val espressoMenus = menuProvider.getForBot("espresso_bot")
        assertThat(espressoMenus.map { it.command })
            .contains("/start", "/status", "/settings", "/settings_profile")

        val childMenus = menuProvider.getByParent("/settings", "espresso_bot")
        assertThat(childMenus).allMatch { it.parent == "/settings" }
    }

    @Test
    fun `telegram logger writes configured bot name`(capturedOutput: CapturedOutput) {
        telegramLogger.run()

        assertThat(capturedOutput.out)
            .contains("Telegram boot 'test-bot' is ready to receive updates")
        assertThat(telegramBootService.botName()).isEqualTo("test-bot")
    }
}
