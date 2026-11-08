package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.domain.bot.BotChatType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MaxUpdateMapperTest {
    private val mapper = MaxUpdateMapper(ObjectMapper())

    @Test
    fun `maps MAX message into private provider neutral update`() {
        val update =
            mapper.from(
                """
                {
                  "update_type": "message_created",
                  "timestamp": 1730000000000,
                  "message": {
                    "sender": {
                      "user_id": 701,
                      "username": "driver",
                      "first_name": "Alex",
                      "last_name": "Driver"
                    },
                    "recipient": { "chat_id": 9001, "chat_type": "dialog" },
                    "body": { "mid": "max-message-1", "text": "/start" }
                  }
                }
                """.trimIndent(),
            )

        val message = assertNotNull(update.message)
        assertEquals("1730000000000", update.updateId)
        assertEquals("max-message-1", message.messageId)
        assertEquals(701, message.chat?.id)
        assertEquals(BotChatType.PRIVATE, message.chat?.type)
        assertEquals(701, message.from?.id)
        assertEquals("Alex", message.from?.firstName)
        assertEquals("/start", message.text)
    }

    @Test
    fun `maps MAX callback payload and source message`() {
        val update =
            mapper.from(
                """
                {
                  "update_type": "message_callback",
                  "timestamp": 1730000000001,
                  "callback": {
                    "callback_id": "callback-1",
                    "payload": "confirm",
                    "user": { "user_id": 702, "first_name": "Sam" }
                  },
                  "message": {
                    "sender": { "user_id": 900, "first_name": "Bot", "is_bot": true },
                    "recipient": { "chat_id": 9002, "chat_type": "dialog" },
                    "body": { "mid": "max-message-2", "text": "Choose" }
                  }
                }
                """.trimIndent(),
            )

        val callback = assertNotNull(update.callbackQuery)
        assertEquals("callback-1", callback.id)
        assertEquals("confirm", callback.data)
        assertEquals(702, callback.from?.id)
        assertEquals("max-message-2", callback.message?.messageId)
        assertEquals(702, callback.message?.chat?.id)
        assertEquals(BotChatType.PRIVATE, callback.message?.chat?.type)
    }

    @Test
    fun `maps incoming MAX location attachment`() {
        val update =
            mapper.from(
                """
                {
                  "update_type": "message_created",
                  "message": {
                    "sender": { "user_id": 701 },
                    "recipient": { "chat_type": "dialog" },
                    "body": { "attachments": [{ "type": "location", "payload": { "lat": 60.1, "lon": 30.2 } }] }
                  }
                }
                """.trimIndent(),
            )

        assertEquals(60.1, update.message?.location?.latitude)
        assertEquals(30.2, update.message?.location?.longitude)
    }
}
