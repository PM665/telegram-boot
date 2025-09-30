package io.github.pm665.telegramboot.bot.autoconfigure

import io.github.pm665.telegramboot.domain.configuration.TelegramBootProperties
import io.github.pm665.telegramboot.domain.telegram.Bot
import io.github.pm665.telegramboot.domain.telegram.BotChat
import io.github.pm665.telegramboot.domain.telegram.BotUser
import io.github.pm665.telegramboot.domain.telegram.CalledCommand
import io.github.pm665.telegramboot.domain.telegram.Command
import io.github.pm665.telegramboot.domain.telegram.Menu
import io.github.pm665.telegramboot.domain.telegram.Message
import io.github.pm665.telegramboot.domain.telegram.Role
import io.github.pm665.telegramboot.domain.telegram.TelegramBootService
import io.github.pm665.telegramboot.domain.telegram.UserRole
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
    fun `backs off when user provides custom in-memory beans`() {
        assertBacksOff(BotProvider::class.java, CustomBotProviderConfiguration::class.java, TestBotProvider::class.java)
        assertBacksOff(BotUserProvider::class.java, CustomBotUserProviderConfiguration::class.java, TestBotUserProvider::class.java)
        assertBacksOff(BotChatProvider::class.java, CustomBotChatProviderConfiguration::class.java, TestBotChatProvider::class.java)
        assertBacksOff(CommandProvider::class.java, CustomCommandProviderConfiguration::class.java, TestCommandProvider::class.java)
        assertBacksOff(
            CalledCommandProvider::class.java,
            CustomCalledCommandProviderConfiguration::class.java,
            TestCalledCommandProvider::class.java,
        )
        assertBacksOff(MenuProvider::class.java, CustomMenuProviderConfiguration::class.java, TestMenuProvider::class.java)
        assertBacksOff(UserRoleProvider::class.java, CustomUserRoleProviderConfiguration::class.java, TestUserRoleProvider::class.java)
        assertBacksOff(MessageProvider::class.java, CustomMessageProviderConfiguration::class.java, TestMessageProvider::class.java)
    }

    @Test
    fun `backs off when user provides telegram boot service`() {
        val customService = TelegramBootService(TelegramBootProperties().apply { name = "custom" })

        contextRunner
            .withPropertyValues("telegram-boot.enabled=true")
            .withBean(TelegramBootService::class.java, Supplier { customService })
            .run { context ->
                assertThat(context.getBean(TelegramBootService::class.java)).isSameAs(customService)
            }
    }

    private fun <T : Any> assertBacksOff(
        beanType: Class<T>,
        configurationClass: Class<*>,
        expectedType: Class<out T>,
    ) {
        contextRunner
            .withUserConfiguration(configurationClass)
            .run { context ->
                val beans = context.getBeansOfType(beanType)
                assertThat(beans).hasSize(1)
                assertThat(beans.values.single()).isInstanceOf(expectedType)
            }
    }

    private class TestBotProvider : BotProvider {
        override fun getBots(): Collection<Bot> = emptyList()

        override fun addBot(bot: Bot) = Unit

        override fun removeBot(botUsername: String) = Unit
    }

    private class TestBotUserProvider : BotUserProvider {
        override fun getBotUsers(): Collection<BotUser> = emptyList()

        override fun addBotUser(botUser: BotUser) = Unit

        override fun removeBotUser(botUserId: Long) = Unit
    }

    private class TestBotChatProvider : BotChatProvider {
        override fun getBotChats(): Collection<BotChat> = emptyList()

        override fun getForBot(botUsername: String): Collection<BotChat> = emptyList()

        override fun addBotChat(botChat: BotChat) = Unit

        override fun removeBotChat(botChatId: Long) = Unit
    }

    private class TestCommandProvider : CommandProvider {
        override fun getCommands(): Collection<Command> = emptyList()

        override fun getForBot(botUsername: String): Collection<Command> = emptyList()

        override fun getByCommand(
            commandName: String,
            botUsername: String,
        ): Command? = null

        override fun addCommand(command: Command) = Unit

        override fun removeCommand(
            commandName: String,
            botUsername: String,
        ) = Unit
    }

    private class TestCalledCommandProvider : CalledCommandProvider {
        override fun getCalledCommands(): Collection<CalledCommand> = emptyList()

        override fun getForBot(botUsername: String): Collection<CalledCommand> = emptyList()

        override fun getForChat(
            botUsername: String,
            chatId: Long,
        ): Collection<CalledCommand> = emptyList()

        override fun addCalledCommand(calledCommand: CalledCommand) = Unit
    }

    private class TestMenuProvider : MenuProvider {
        override fun getMenus(): Collection<Menu> = emptyList()

        override fun getForBot(botUsername: String): Collection<Menu> = emptyList()

        override fun getByParent(
            parent: String?,
            botUsername: String,
        ): Collection<Menu> = emptyList()

        override fun addMenu(menu: Menu) = Unit

        override fun removeMenu(
            command: String,
            botUsername: String,
        ) = Unit
    }

    private class TestUserRoleProvider : UserRoleProvider {
        override fun getUserRoles(): Collection<UserRole> = emptyList()

        override fun getForBot(botUsername: String): Collection<UserRole> = emptyList()

        override fun addUserRole(userRole: UserRole) = Unit

        override fun removeUserRole(
            botUsername: String?,
            botUserId: Long,
            role: Role,
        ) = Unit
    }

    private class TestMessageProvider : MessageProvider {
        override fun getMessages(): Collection<Message> = emptyList()

        override fun getByBotAndChat(
            botUsername: String,
            chatId: Long,
        ): Collection<Message> = emptyList()

        override fun addMessage(message: Message) = Unit

        override fun removeMessage(messageId: String) = Unit
    }

    @org.springframework.context.annotation.Configuration
    private class CustomBotProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun botProvider(): BotProvider = TestBotProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomBotUserProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun botUserProvider(): BotUserProvider = TestBotUserProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomBotChatProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun botChatProvider(): BotChatProvider = TestBotChatProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomCommandProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun commandProvider(): CommandProvider = TestCommandProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomCalledCommandProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun calledCommandProvider(): CalledCommandProvider = TestCalledCommandProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomMenuProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun menuProvider(): MenuProvider = TestMenuProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomUserRoleProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun userRoleProvider(): UserRoleProvider = TestUserRoleProvider()
    }

    @org.springframework.context.annotation.Configuration
    private class CustomMessageProviderConfiguration {
        @org.springframework.context.annotation.Bean
        fun messageProvider(): MessageProvider = TestMessageProvider()
    }
}
