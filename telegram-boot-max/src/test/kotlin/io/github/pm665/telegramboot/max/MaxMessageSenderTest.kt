package io.github.pm665.telegramboot.max

import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender.SendMessageContext
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class MaxMessageSenderTest {
    @Test
    fun `preserves structured message arguments when sending action messages`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        val order = Any()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer {
            assertThat(it.getArgument<Map<String, *>>(2)["foundOrder"]).isSameAs(order)
            "order details"
        }
        server.expect(requestTo("https://platform-api2.max.ru/messages?user_id=701"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"text":"order details","format":"markdown"}"""))
            .andRespond(withSuccess("""{"message":{"body":{"mid":"order-message"}}}""", MediaType.APPLICATION_JSON))
        val sender =
            MaxMessageSender(
                botsService,
                MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()),
                mock(),
                textProcessor,
                MaxMessageRateLimiter(),
            )

        sender.sendMessages(
            SendMessageContext
                .builder()
                .botUsername("max-driver")
                .chat(701)
                .messageToSend(SendMessageMarkup("pointB.priceInfo"))
                .messageArg("foundOrder", order)
                .build(),
        )

        server.verify()
    }

    @Test
    fun `skips media-only MAX message instead of sending an empty message`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender =
            MaxMessageSender(
                botsService,
                MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()),
                mock(),
                textProcessor,
                MaxMessageRateLimiter(),
            )

        val result =
            sender.sendMessage(
                SendMessageMarkup(
                    text = "",
                    photo = SendMessageMarkup.Photo.Bytes("image.jpg", "image/jpeg", byteArrayOf(1, 2)),
                ),
                chatId = 701,
                botUsername = "max-driver",
                messageArgs = emptyMap(),
            )

        assertEquals(null, result)
        server.verify()
    }

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `sends text and inline keyboard without leaking Telegram media into MAX request`(output: CapturedOutput) {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val usersService = mock<UsersService>()
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer {
            it.getArgument(0)
        }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), usersService, textProcessor, MaxMessageRateLimiter())
        val keyboard = SendMessageMarkup.MenuMarkup().addButtonToLastRow("Confirm", callbackData = "confirm")

        server.expect(requestTo("https://platform-api2.max.ru/messages?user_id=701"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "secret-token"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "text": "driver answer",
                      "format": "markdown",
                      "attachments": [
                        {
                          "type": "inline_keyboard",
                          "payload": {
                            "buttons": [[{"type": "callback", "text": "Confirm", "payload": "confirm"}]]
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            )
            .andRespond(
                withSuccess(
                    """{"message":{"body":{"mid":"max-message-2"},"recipient":{"chat_id":701}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result =
            sender.sendMessage(
                SendMessageMarkup(
                    text = "driver answer",
                    photo = SendMessageMarkup.Photo.Bytes("image.jpg", "image/jpeg", byteArrayOf(1, 2)),
                    menuMarkup = keyboard,
                ),
                chatId = 701,
                botUsername = "max-driver",
                messageArgs = emptyMap(),
            )

        assertEquals("max-message-2", result?.messageId)
        assertEquals(701, result?.chatId)
        assertThat(output.out).contains("Skipping media for MAX bot=max-driver chatId=701")
        server.verify()
    }

    @Test
    fun `acknowledges callback through MAX callback endpoint`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val sender =
            MaxMessageSender(
                botsService,
                MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()),
                mock(),
                mock(),
                MaxMessageRateLimiter(),
            )
        server.expect(requestTo("https://platform-api2.max.ru/answers?callback_id=callback-1"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "secret-token"))
            .andRespond(withSuccess())

        sender.sendCallbackNotification(
            BotUpdate(callbackQuery = BotCallbackQuery("callback-1", "confirm", null, null)),
            "Confirmed",
            botUsername = "max-driver",
        )

        server.verify()
    }

    @Test
    fun `sends location as MAX location attachment`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), mock(), MaxMessageRateLimiter())
        server.expect(requestTo("https://platform-api2.max.ru/messages?user_id=701"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"attachments":[{"type":"location","latitude":60.1,"longitude":30.2}]}"""))
            .andRespond(withSuccess("""{"message":{"body":{"mid":"location-1"}}}""", MediaType.APPLICATION_JSON))

        val result = sender.sendLocation(60.1, 30.2, 701, "max-driver")

        assertEquals("location-1", result?.messageId)
        server.verify()
    }

    @Test
    fun `maps request location to MAX geo request button`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())
        server.expect(requestTo("https://platform-api2.max.ru/messages?user_id=701"))
            .andExpect(content().json("""{"text":"share","format":"markdown","attachments":[{"type":"inline_keyboard","payload":{"buttons":[[{"type":"request_geo_location","text":"common.shareLocation"}]]}}]}"""))
            .andRespond(withSuccess("""{"message":{"body":{"mid":"request-1"}}}""", MediaType.APPLICATION_JSON))

        val result = sender.sendMessage(SendMessageMarkup(text = "share", requestLocation = true), 701, "max-driver", emptyMap())

        assertEquals("request-1", result?.messageId)
        server.verify()
    }

    @Test
    fun `rejects text over MAX limit before sending`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())

        assertThrows<IllegalArgumentException> {
            sender.sendMessage(SendMessageMarkup(text = "😀".repeat(4001)), 701, "max-driver", emptyMap())
        }

        server.verify()
    }

    @Test
    fun `rejects keyboard shapes outside MAX limits before sending`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())
        val keyboard = SendMessageMarkup.MenuMarkup(rows = mutableListOf(SendMessageMarkup.MenuRow((1..8).map { SendMessageMarkup.MenuButton("Button $it", "$it") }.toMutableList())))

        assertThrows<IllegalArgumentException> {
            sender.sendMessage(SendMessageMarkup(text = "buttons", menuMarkup = keyboard), 701, "max-driver", emptyMap())
        }

        server.verify()
    }

    @Test
    fun `rejects more than three link buttons in one row before sending`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())
        val links = (1..4).map { SendMessageMarkup.MenuButton("Link $it", url = "https://example.com/$it") }.toMutableList()
        val keyboard = SendMessageMarkup.MenuMarkup(rows = mutableListOf(SendMessageMarkup.MenuRow(links)))

        assertThrows<IllegalArgumentException> {
            sender.sendMessage(SendMessageMarkup(text = "links", menuMarkup = keyboard), 701, "max-driver", emptyMap())
        }

        server.verify()
    }

    @Test
    fun `rejects callback message edit text over MAX limit`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor = mock<MessageTextProcessor>()
        whenever(textProcessor.prepareText(any(), any(), any())).thenAnswer { it.getArgument(0) }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())

        assertThrows<IllegalArgumentException> {
            sender.editMessage(
                "max-driver",
                BotUpdate(
                    callbackQuery =
                        BotCallbackQuery(
                            "callback-1",
                            "confirm",
                            null,
                            io.github.pm665.telegramboot.domain.bot.BotIncomingMessage(
                                chat = io.github.pm665.telegramboot.domain.bot.BotChatInfo(701, io.github.pm665.telegramboot.domain.bot.BotChatType.PRIVATE),
                            ),
                        ),
                ),
                "😀".repeat(4001),
                null,
                emptyMap(),
            )
        }

        server.verify()
    }

    @Test
    fun `registers bot command hints with MAX`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        val botsService = InMemoryBotsService().apply { register(BotsService.Bot("secret-token", "max-driver", BotPlatform.MAX)) }
        val textProcessor =
            object : MessageTextProcessor {
                override fun prepareText(text: String, locale: String?, messageArgs: Map<String, *>): String = "Start the bot"
            }
        val sender = MaxMessageSender(botsService, MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()), mock(), textProcessor, MaxMessageRateLimiter())
        server.expect(requestTo("https://platform-api2.max.ru/me/commands"))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(header("Authorization", "secret-token"))
            .andExpect(content().json("""{"commands":[{"name":"start","description":"Start the bot"}]}"""))
            .andRespond(withSuccess())

        sender.sendCommands("max-driver", mapOf("start" to "start.command"))

        server.verify()
    }
}
