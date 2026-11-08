package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotLocation
import io.github.pm665.telegramboot.domain.bot.BotMessageEntity
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChatUtilsTest {
    @Test
    fun `extractFormattedMessage builds HTML from entities`() {
        val update =
            BotUpdate(
                message =
                    BotIncomingMessage(
                        text = "Hi bold",
                        entities = listOf(BotMessageEntity("bold", 3, 4)),
                    ),
            )

        val formatted = ChatUtils.extractFormattedMessage(update)

        assertThat(formatted).isEqualTo("Hi <b>bold</b>")
    }

    @Test
    fun `extractFormattedMessage returns plain text when no entities`() {
        val update = BotUpdate(message = BotIncomingMessage(text = "simple text"))

        val formatted = ChatUtils.extractFormattedMessage(update)

        assertThat(formatted).isEqualTo("simple text")
    }

    @Test
    fun `extractFormattedMessage escapes text and link attributes`() {
        val update = BotUpdate(message = BotIncomingMessage(
            text = "A & link <tag>",
            entities = listOf(BotMessageEntity("text_link", 4, 4, url = "https://example.com/?a=1&b=2")),
        ))

        assertThat(ChatUtils.extractFormattedMessage(update))
            .isEqualTo("A &amp; <a href=\"https://example.com/?a=1&amp;b=2\">link</a> &lt;tag&gt;")
        assertThat(ChatUtils.extractFormattedMessage(BotUpdate(message = BotIncomingMessage(text = "< & >"))))
            .isEqualTo("&lt; &amp; &gt;")
    }

    @Test
    fun `extractFormattedMessage returns empty string for callback updates`() {
        val update = BotUpdate(callbackQuery = BotCallbackQuery("cb-id", "cb", BotUserInfo(1L), null))

        val formatted = ChatUtils.extractFormattedMessage(update)

        assertThat(formatted).isEmpty()
    }

    @Test
    fun `extractUser tolerates missing language`() {
        val update =
            BotUpdate(
                message =
                    BotIncomingMessage(
                        chat = BotChatInfo(1L, BotChatType.PRIVATE),
                        from = BotUserInfo(1L, "tg_alice"),
                    ),
            )

        val user = ChatUtils.extractUser(update)

        assertThat(user).isEqualTo(io.github.pm665.telegramboot.ports.UsersService.User(1L, "tg_alice", ""))
    }

    @Test
    fun `extractChatId prefers callback message chat id over user id`() {
        val update =
            BotUpdate(
                callbackQuery =
                    BotCallbackQuery(
                        id = "cb-id",
                        data = "cb",
                        from = BotUserInfo(99L, "Alice"),
                        message = BotIncomingMessage(chat = BotChatInfo(42L, BotChatType.PRIVATE)),
                    ),
            )

        val chatId = ChatUtils.extractChatId(update)

        assertThat(chatId).isEqualTo(42L)
    }

    @Test
    fun `createPayloadData extracts neutral location and callback state`() {
        val location = BotLocation(55.75, 37.62)
        val update = BotUpdate(message = BotIncomingMessage(location = location))

        val payload = ChatUtils.createPayloadData(update)

        assertThat(payload.location).isEqualTo(location)
        assertThat(payload.isCallback).isFalse()
    }
}
