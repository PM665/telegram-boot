package io.github.pm665.telegramboot.bot.autoconfigure

import io.github.pm665.telegramboot.domain.configuration.TelegramBootProperties
import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import io.github.pm665.telegramboot.ports.BotChatProvider
import io.github.pm665.telegramboot.ports.BotProvider
import io.github.pm665.telegramboot.ports.BotUserProvider
import io.github.pm665.telegramboot.ports.CalledCommandProvider
import io.github.pm665.telegramboot.ports.CommandProvider
import io.github.pm665.telegramboot.ports.MenuProvider
import io.github.pm665.telegramboot.ports.MessageProvider
import io.github.pm665.telegramboot.ports.UserRoleProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier

class TelegramBootServiceAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TelegramBootServiceAutoConfiguration::class.java))

    @Test
    fun `registers default in-memory providers`() {
        contextRunner.run { context ->
            assertThat(context.getBeansOfType(BotProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(BotUserProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(BotChatProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(CommandProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(CalledCommandProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(MenuProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(UserRoleProvider::class.java)).hasSize(1)
            assertThat(context.getBeansOfType(MessageProvider::class.java)).hasSize(1)
        }
    }

    @Test
    fun `binds telegram boot properties`() {
        contextRunner
            .withPropertyValues(
                "telegram-boot.enabled=true",
                "telegram-boot.name=myBot",
            )
            .run { context ->
                val properties = context.getBean(TelegramBootProperties::class.java)
                assertThat(properties.name).isEqualTo("myBot")
                assertThat(properties.enabled).isTrue()
            }
    }

    @Nested
    inner class TelegramBootServiceCreation {
        @Test
        fun `creates telegram boot service when enabled`() {
            contextRunner
                .withPropertyValues("telegram-boot.enabled=true")
                .run { context ->
                    assertThat(context.getBeansOfType(TelegramBootService::class.java)).hasSize(1)
                }
        }

        @Test
        fun `does not create telegram boot service when disabled`() {
            contextRunner
                .withPropertyValues("telegram-boot.enabled=false")
                .run { context ->
                    assertThat(context.getBeansOfType(TelegramBootService::class.java)).isEmpty()
                }
        }

        @Test
        fun `does not create telegram boot service when property missing`() {
            contextRunner.run { context ->
                assertThat(context.getBeansOfType(TelegramBootService::class.java)).isEmpty()
            }
        }
    }

    @Test
    fun `backs off when user provides custom bot provider`() {
        contextRunner
            .withBean(BotProvider::class.java, Supplier { TestBotProvider() })
            .run { context ->
                val beans = context.getBeansOfType(BotProvider::class.java)
                assertThat(beans).hasSize(1)
                assertThat(beans.values.single()).isInstanceOf(TestBotProvider::class.java)
            }
    }

    private class TestBotProvider : BotProvider {
        override fun getBots(): Collection<Bot> = emptyList()

        override fun addBot(bot: Bot) = Unit

        override fun removeBot(botUsername: String) = Unit
    }
}
