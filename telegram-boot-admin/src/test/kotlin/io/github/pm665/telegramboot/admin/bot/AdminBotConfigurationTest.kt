package io.github.pm665.telegramboot.admin.bot

import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class AdminBotConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AdminBotConfiguration::class.java)
            .withBean("adminBotNavigation", AdminBotNavigation::class.java, { AdminBotNavigation() })
            .withBean(UsersService::class.java, { mock(UsersService::class.java) })
            .withBean(PermissionsService::class.java, { mock(PermissionsService::class.java) })

    @Test
    fun `does not create webhook bot without both credentials`() {
        contextRunner
            .withPropertyValues("telegram-boot.admin.telegram-login-bot-token=test-token")
            .run { context ->
                assertThat(context).doesNotHaveBean(AdminBot::class.java)
            }
    }

    @Test
    fun `creates webhook bot with existing credentials`() {
        contextRunner
            .withPropertyValues(
                "telegram-boot.admin.telegram-login-bot-token=test-token",
                "telegram-boot.admin.telegram-login-bot-username=admin_bot",
            ).run { context ->
                assertThat(context).hasSingleBean(AdminBot::class.java)
            }
    }

    @Test
    fun `does not register bot on context startup`() {
        contextRunner
            .withPropertyValues(
                "telegram-boot.admin.telegram-login-bot-token=test-token",
                "telegram-boot.admin.telegram-login-bot-username=admin_bot",
            ).run { context ->
                assertThat(context).doesNotHaveBean(ApplicationRunner::class.java)
            }
    }
}
