package io.github.pm665.telegramboot.admin.bot

import com.sun.net.httpserver.HttpServer
import io.github.pm665.telegramboot.events.AdminBotNavigation
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.net.InetSocketAddress

class AdminBotConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AdminBotConfiguration::class.java)
            .withBean("adminBotNavigation", AdminBotNavigation::class.java, { AdminBotNavigation() })
            .withBean(UsersService::class.java, { mock(UsersService::class.java) })
            .withBean(PermissionsService::class.java, { mock(PermissionsService::class.java) })

    @Test
    fun `admin bot uses its configured api origin`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/bottest-token/getme") { request ->
            val body = "{\"ok\":true,\"result\":{\"id\":1,\"is_bot\":true,\"first_name\":\"Admin\",\"username\":\"admin_bot\"}}".toByteArray()
            request.sendResponseHeaders(200, body.size.toLong())
            request.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            contextRunner.withPropertyValues(
                "telegram-boot.admin.telegram-login-bot-token=test-token",
                "telegram-boot.admin.telegram-login-bot-username=admin_bot",
                "telegram-boot.telegram.api-url=http://127.0.0.1:1",
                "telegram-boot.admin.telegram-api-url=http://127.0.0.1:${server.address.port}",
            ).run { context ->
                assertThat(context.getBean(TelegramClient::class.java).execute(GetMe()).userName).isEqualTo("admin_bot")
            }
        } finally {
            server.stop(0)
        }
    }

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
