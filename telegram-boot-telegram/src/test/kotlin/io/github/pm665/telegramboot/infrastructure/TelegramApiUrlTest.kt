package io.github.pm665.telegramboot.infrastructure

import com.sun.net.httpserver.HttpServer
import io.github.pm665.telegramboot.TelegramBootTelegramAutoConfiguration
import io.github.pm665.telegramboot.domain.configuration.TelegramProperties
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.drivers.telegram.BotsFactory
import io.github.pm665.telegramboot.drivers.telegram.DefaultActionOperatorBot
import io.github.pm665.telegramboot.features.messaging.TelegramMessageSender
import io.github.pm665.telegramboot.ports.BotsService.Bot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TelegramApiUrlTest {
    @Test
    fun `configured origin is used for token validation, requests and polling`() {
        val polling = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { request ->
            val result = when (request.requestURI.path.lowercase()) {
                "/bottoken/getme" -> "{\"id\":1,\"is_bot\":true,\"first_name\":\"Test\",\"username\":\"test_bot\"}"
                "/bottoken/getmycommands" -> "[]"
                "/bottoken/deletewebhook" -> "true"
                "/bottoken/getupdates" -> {
                    polling.countDown()
                    "[]"
                }
                else -> error("Unexpected request: ${request.requestURI}")
            }
            val body = "{\"ok\":true,\"result\":$result}".toByteArray()
            request.sendResponseHeaders(200, body.size.toLong())
            request.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val url = TelegramBootTelegramAutoConfiguration().telegramApiUrl(TelegramProperties("http://127.0.0.1:${server.address.port}"))
            TelegramPlatformBotTokenValidator(url).validate("token", "test_bot")
            val bot = DefaultActionOperatorBot("token", "test_bot", mock(BotUpdateHandler::class.java), url)
            assertThat(bot.execute(GetMyCommands())).isEmpty()
            val factory = mock(BotsFactory::class.java)
            `when`(factory.getClient("test_bot")).thenReturn(bot)
            TelegramBotsLongPollingApplication().use { application ->
                val manager = BotManager(application, factory, mock(TelegramMessageSender::class.java), url)
                val details = Bot("token", "test_bot")
                manager.startBot(details)
                assertThat(polling.await(5, TimeUnit.SECONDS)).isTrue()
                manager.stopBot(details)
            }
        } finally {
            server.stop(0)
        }
    }
}
