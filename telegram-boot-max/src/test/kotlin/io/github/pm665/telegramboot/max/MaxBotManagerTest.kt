package io.github.pm665.telegramboot.max

import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.events.NgrokUrlAvailableEvent
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.ports.BotsService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class MaxBotManagerTest {
    @Test
    fun `validates token then subscribes only to command and callback updates`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val apiClient = MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper())
        val properties = MaxProperties().apply { webhookBaseUrl = "https://bots.example.test/" }
        val messageSender = mock<MaxMessageSender>()
        val manager = MaxBotManager(properties, apiClient, InMemoryBotsService(), messageSender)
        val bot = BotsService.Bot("max-token", "max-driver", BotPlatform.MAX)

        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "max-token"))
            .andRespond(withSuccess("""{"username":"max-driver"}""", MediaType.APPLICATION_JSON))
        server.expect(requestTo("https://platform-api2.max.ru/subscriptions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "max-token"))
            .andExpect(
                content().json(
                    """
                    {
                      "url": "https://bots.example.test/max/bot/max-driver",
                      "update_types": ["message_created", "message_callback"],
                      "secret": "${MaxWebhookSecret.fromToken("max-token")}"
                    }
                    """.trimIndent(),
                ),
            )
            .andRespond(withSuccess("""{"success":true}""", MediaType.APPLICATION_JSON))

        manager.startBot(bot)

        verify(messageSender).sendCommands("max-driver", mapOf("start" to "start.command"))
        server.verify()
    }

    @Test
    fun `subscribes active MAX bots when embedded ngrok URL becomes available`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply {
            register(BotsService.Bot("max-token", "max-driver", BotPlatform.MAX))
            register(BotsService.Bot("telegram-token", "telegram-driver", BotPlatform.TELEGRAM))
        }
        val messageSender = mock<MaxMessageSender>()
        val manager = MaxBotManager(MaxProperties(), MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), botsService, messageSender)

        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andExpect(header("Authorization", "max-token"))
            .andRespond(withSuccess("""{"username":"max-driver"}""", MediaType.APPLICATION_JSON))
        server.expect(requestTo("https://platform-api2.max.ru/subscriptions"))
            .andExpect(header("Authorization", "max-token"))
            .andExpect(
                content().json(
                    """
                    {
                      "url": "https://current.ngrok.app/max/bot/max-driver",
                      "update_types": ["message_created", "message_callback"],
                      "secret": "${MaxWebhookSecret.fromToken("max-token")}"
                    }
                    """.trimIndent(),
                ),
            )
            .andRespond(withSuccess("""{"success":true}""", MediaType.APPLICATION_JSON))

        manager.onNgrokUrlAvailable(NgrokUrlAvailableEvent("https://current.ngrok.app"))

        verify(messageSender).sendCommands("max-driver", mapOf("start" to "start.command"))
        server.verify()
    }
}
