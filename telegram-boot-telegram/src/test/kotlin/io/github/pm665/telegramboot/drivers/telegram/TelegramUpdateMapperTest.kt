package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatMember
import io.github.pm665.telegramboot.domain.bot.BotChatMemberStatus
import io.github.pm665.telegramboot.domain.bot.BotChatMemberUpdate
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotLocation
import io.github.pm665.telegramboot.domain.bot.BotMessageEntity
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.MessageEntity
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.location.Location
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.time.Instant

class TelegramUpdateMapperTest {
    private val mapper = TelegramUpdateMapper()

    @Test
    fun `maps private message fields into bot update`() {
        val update =
            Update().apply {
                updateId = 123
                message =
                    Message().apply {
                        messageId = 456
                        chat = Chat(789L, "private")
                        from =
                            User(987L, "Alex", false).apply {
                                userName = "alex_driver"
                                languageCode = "ru"
                                lastName = "Rider"
                            }
                        text = "Order 42"
                        entities = listOf(MessageEntity("text_link", 0, 5).apply { url = "https://example.com" })
                        location = Location(37.62, 55.75)
                    }
            }

        val result = mapper.from(update)

        assertThat(result).isEqualTo(
            BotUpdate(
                updateId = "123",
                message =
                    BotIncomingMessage(
                        messageId = "456",
                        chat = BotChatInfo(789L, BotChatType.PRIVATE),
                        from = BotUserInfo(987L, "alex_driver", "ru", "Alex", "Rider"),
                        text = "Order 42",
                        entities = listOf(BotMessageEntity("text_link", 0, 5, "https://example.com")),
                        location = BotLocation(55.75, 37.62),
                    ),
            ),
        )
    }

    @Test
    fun `maps callback query and source message`() {
        val update =
            Update().apply {
                updateId = 124
                callbackQuery =
                    CallbackQuery().apply {
                        id = "callback-1"
                        data = "register|state|confirm"
                        from = User(987L, "Alex", false).apply {
                            userName = "alex_driver"
                            languageCode = "ru"
                            lastName = "Rider"
                        }
                        message =
                            Message().apply {
                                messageId = 456
                                chat = Chat(789L, "private")
                            }
                    }
            }

        val result = mapper.from(update)

        assertThat(result).isEqualTo(
            BotUpdate(
                updateId = "124",
                callbackQuery =
                    BotCallbackQuery(
                        id = "callback-1",
                        data = "register|state|confirm",
                        from = BotUserInfo(987L, "alex_driver", "ru", "Alex", "Rider"),
                        message = BotIncomingMessage(
                            messageId = "456",
                            chat = BotChatInfo(789L, BotChatType.PRIVATE),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `maps callback query without source message`() {
        val update =
            Update().apply {
                callbackQuery =
                    CallbackQuery().apply {
                        id = "inline-callback"
                        data = "confirm"
                        from = User(987L, "Alex", false)
                    }
            }

        val result = mapper.from(update)

        assertThat(result.callbackQuery).isEqualTo(
            BotCallbackQuery(
                id = "inline-callback",
                data = "confirm",
                from = BotUserInfo(987L, firstName = "Alex"),
                message = null,
            ),
        )
    }

    @Test
    fun `maps my chat member status and posting permission`() {
        val updateDate = Instant.parse("2026-10-07T12:00:00Z")
        val update =
            Update().apply {
                myChatMember =
                    ChatMemberUpdated().apply {
                        chat = Chat(-100L, "channel").apply {
                            title = "Drivers"
                            userName = "drivers_channel"
                        }
                        date = updateDate.epochSecond.toInt()
                        newChatMember = ChatMemberAdministrator().apply { canPostMessages = true }
                    }
            }

        val result = mapper.from(update)

        assertThat(result.myChatMember).isEqualTo(
            BotChatMemberUpdate(
                chat = BotChatInfo(-100L, BotChatType.CHANNEL, "Drivers", "drivers_channel"),
                date = updateDate,
                newMember = BotChatMember(BotChatMemberStatus.ADMINISTRATOR, canPostMessages = true),
            ),
        )
    }
}
