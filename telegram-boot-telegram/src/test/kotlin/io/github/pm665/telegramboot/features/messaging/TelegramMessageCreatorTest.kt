package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.features.callbacks.CallbackData
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

class TelegramMessageCreatorTest {
    @Test
    fun `createBotApiMessage sends text as HTML`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())
        val message = creator.createBotApiMessage(SendMessageMarkup("<b>Hello</b>"), emptyMap(), 42L, "bot") as SendMessage

        assertThat(message.parseMode).isEqualTo("html")
        assertThat(message.text).isEqualTo("<b>Hello</b>")
    }

    @Test
    fun `createBotApiMessage builds sendPhoto for uploaded bytes with caption and keyboard`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())
        val markup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.Bytes("banner.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
                .inlineKeyboardMarkup(
                    SendMessageMarkup.MenuMarkup(
                        rows =
                            mutableListOf(
                                SendMessageMarkup.MenuRow(
                                    mutableListOf(SendMessageMarkup.MenuButton("confirm.reply_yes", "callback_yes")),
                                ),
                            ),
                    ),
                ).build()

        val method = creator.createBotApiMessage(markup, emptyMap(), 42L, "bot")

        assertThat(method).isInstanceOf(SendPhoto::class.java)
        val sendPhoto = method as SendPhoto
        assertThat(sendPhoto.chatId).isEqualTo("42")
        assertThat(sendPhoto.caption).isEqualTo("announcement.text")
        assertThat(sendPhoto.parseMode).isEqualTo("HTML")
        assertThat(sendPhoto.photo.mediaName).isEqualTo("banner.jpg")
        assertThat(sendPhoto.photo.newMediaStream).isNotNull
        assertThat(sendPhoto.replyMarkup).isInstanceOf(InlineKeyboardMarkup::class.java)
        val keyboard = sendPhoto.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(1)
        assertThat(keyboard.keyboard.first()).hasSize(1)
        assertThat(keyboard.keyboard.first().first().callbackData).isEqualTo("callback_yes")
    }

    @Test
    fun `createBotApiMessage rejects uploaded photo bytes without required metadata`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.Bytes("", "image/jpeg", byteArrayOf(1)))
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.Bytes("banner.jpg", "", byteArrayOf(1)))
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.Bytes("banner.jpg", "image/jpeg", byteArrayOf()))
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `createBotApiMessage builds sendPhoto for provider file id with caption and keyboard`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())
        val markup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.ProviderFileId("telegram-file-id"))
                .inlineKeyboardMarkup(
                    SendMessageMarkup.MenuMarkup(
                        rows =
                            mutableListOf(
                                SendMessageMarkup.MenuRow(
                                    mutableListOf(SendMessageMarkup.MenuButton("confirm.reply_yes", "callback_yes")),
                                ),
                            ),
                    ),
                ).build()

        val method = creator.createBotApiMessage(markup, emptyMap(), 42L, "bot")

        assertThat(method).isInstanceOf(SendPhoto::class.java)
        val sendPhoto = method as SendPhoto
        assertThat(sendPhoto.chatId).isEqualTo("42")
        assertThat(sendPhoto.caption).isEqualTo("announcement.text")
        assertThat(sendPhoto.parseMode).isEqualTo("HTML")
        assertThat(sendPhoto.photo.newMediaStream).isNull()
        assertThat(sendPhoto.photo.mediaName).isNull()
        assertThat(sendPhoto.replyMarkup).isInstanceOf(InlineKeyboardMarkup::class.java)
        val keyboard = sendPhoto.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(1)
        assertThat(keyboard.keyboard.first()).hasSize(1)
        assertThat(keyboard.keyboard.first().first().callbackData).isEqualTo("callback_yes")
    }

    @Test
    fun `createBotApiMessage rejects blank telegram file id`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.ProviderFileId(" "))
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `createBotApiMessage rejects non image content type for uploaded photo`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.Bytes("banner.txt", "text/plain", byteArrayOf(1)))
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `createBotApiMessage rejects photo markup that also requests location`() {
        val creator = TelegramMessageCreator(NoopUsersService(), InMemoryCallbackDataService(), IdentityMessageTextProcessor())

        assertThatThrownBy {
            creator.createBotApiMessage(
                SendMessageMarkup
                    .create("announcement.text")
                    .photo(SendMessageMarkup.Photo.Bytes("banner.jpg", "image/jpeg", byteArrayOf(1)))
                    .requestLocation(true)
                    .build(),
                emptyMap(),
                42L,
                "bot",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private class IdentityMessageTextProcessor : MessageTextProcessor {
        override fun prepareText(
            text: String,
            locale: String?,
            messageArgs: Map<String, *>,
        ): String = text
    }

    private class InMemoryCallbackDataService : CallbackDataService {
        private val storage = mutableMapOf<String, CallbackData>()

        override fun findById(id: String): CallbackData? = storage[id]

        override fun createNew(callbackData: CallbackData) {
            storage[callbackData.id] = callbackData
        }
    }

    private class NoopUsersService : UsersService {
        override fun getUser(botUsername: String, userId: Long): UsersService.User? = null

        override fun getUsers(botUsername: String, userIds: Collection<Long>): Collection<UsersService.User> = emptyList()

        override fun findUserByUsername(botUsername: String, username: String): UsersService.User? = null

        override fun getChat(
            botUsername: String,
            chatId: Long,
        ): UsersService.Chat? = null

        override fun getChats(
            botUsername: String,
            userIds: Collection<Long>,
        ): Collection<UsersService.Chat> = emptyList()

        override fun getChatsByBotUserIds(
            botUsername: String,
            botUserIds: Collection<Long>,
        ): Collection<UsersService.Chat> = emptyList()

        override fun getChats(
            botUsername: String,
            accessType: io.github.pm665.telegramboot.domain.AccessType,
        ): Collection<UsersService.Chat> = emptyList()

        override fun createNewUser(
            botUsername: String,
            userId: Long,
            username: String?,
            locale: String,
        ): UsersService.User = UsersService.User(userId, username, locale)

        override fun createNewChat(
            userId: Long,
            botUsername: String,
            chatId: Long,
        ): UsersService.Chat = UsersService.Chat(userId, botUsername, chatId, true)

        override fun setChatEnabled(
            botUsername: String,
            chatId: Long,
            enabled: Boolean,
        ) {}
    }
}
