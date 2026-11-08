package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.domain.actions.SendLocationItem
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.bot.BotFileReference
import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.drivers.telegram.OperatorBot
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendLocation
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable

class TelegramMessageSenderPhotoTest {
    @Test
    fun `sendMessage dispatches photo markup and returns neutral message`() {
        val applicationContext = mock<ApplicationContext>()
        val usersService = mock<UsersService>()
        val messageTextProcessor = mock<MessageTextProcessor>()
        val callbackDataService = mock<CallbackDataService>()
        val creator = TelegramMessageCreator(usersService, callbackDataService, messageTextProcessor)
        val operatorBot = mock<OperatorBot>()
        val sentMessage = Message().apply {
            chat = Chat(42L, "private")
            messageId = 55
        }
        val markup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.Bytes("banner.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
                .build()

        whenever(messageTextProcessor.prepareText("announcement.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, String>()))
            .thenReturn("announcement.text")
        whenever(usersService.getUserChat("bot", 42L)).thenReturn(null)
        whenever(applicationContext.getBean("bot", OperatorBot::class.java)).thenReturn(operatorBot)
        whenever(operatorBot.execute(org.mockito.kotlin.any<SendPhoto>())).thenReturn(sentMessage)

        val sender = TelegramMessageSender(applicationContext, usersService, messageTextProcessor, creator)

        val result = sender.sendMessage(markup, 42L, "bot", emptyMap())

        assertThat(result).isEqualTo(BotMessage(messageId = "55", chatId = 42L))
        verify(operatorBot).execute(org.mockito.kotlin.any<SendPhoto>())
    }

    @Test
    fun `sendMessage returns neutral photo references for provider file id source`() {
        val applicationContext = mock<ApplicationContext>()
        val usersService = mock<UsersService>()
        val messageTextProcessor = mock<MessageTextProcessor>()
        val callbackDataService = mock<CallbackDataService>()
        val creator = TelegramMessageCreator(usersService, callbackDataService, messageTextProcessor)
        val operatorBot = mock<OperatorBot>()
        val sentMessage =
            Message().apply {
                chat = Chat(42L, "private")
                messageId = 55
                photo = listOf(PhotoSize().apply {
                    fileId = "saved-file-id"
                    fileUniqueId = "saved-file-unique-id"
                })
            }
        val markup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.ProviderFileId("telegram-file-id"))
                .build()

        whenever(messageTextProcessor.prepareText("announcement.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, String>()))
            .thenReturn("announcement.text")
        whenever(usersService.getUserChat("bot", 42L)).thenReturn(null)
        whenever(applicationContext.getBean("bot", OperatorBot::class.java)).thenReturn(operatorBot)
        whenever(operatorBot.execute(org.mockito.kotlin.any<SendPhoto>())).thenReturn(sentMessage)

        val sender = TelegramMessageSender(applicationContext, usersService, messageTextProcessor, creator)

        val result = sender.sendMessage(markup, 42L, "bot", emptyMap())

        assertThat(result)
            .isEqualTo(
                BotMessage(
                    messageId = "55",
                    chatId = 42L,
                    files = listOf(BotFileReference("saved-file-id", "saved-file-unique-id")),
                ),
            )
        verify(operatorBot).execute(org.mockito.kotlin.any<SendPhoto>())
    }

    @Test
    fun `sendMessage returns null when telegram rejects valid photo send`() {
        val applicationContext = mock<ApplicationContext>()
        val usersService = mock<UsersService>()
        val messageTextProcessor = mock<MessageTextProcessor>()
        val callbackDataService = mock<CallbackDataService>()
        val creator = TelegramMessageCreator(usersService, callbackDataService, messageTextProcessor)
        val operatorBot = mock<OperatorBot>()
        val markup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.ProviderFileId("telegram-file-id"))
                .build()

        whenever(messageTextProcessor.prepareText("announcement.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, String>()))
            .thenReturn("announcement.text")
        whenever(usersService.getUserChat("bot", 42L)).thenReturn(null)
        whenever(applicationContext.getBean("bot", OperatorBot::class.java)).thenReturn(operatorBot)
        whenever(operatorBot.execute(org.mockito.kotlin.any<SendPhoto>())).thenThrow(RuntimeException(TelegramApiException("boom")))

        val sender = TelegramMessageSender(applicationContext, usersService, messageTextProcessor, creator)

        val result = sender.sendMessage(markup, 42L, "bot", emptyMap())

        assertThat(result).isNull()
        verify(operatorBot).execute(org.mockito.kotlin.any<SendPhoto>())
    }

    @Test
    fun `sendMessages keeps text only batch behavior across multiple chats`() {
        val applicationContext = mock<ApplicationContext>()
        val usersService = mock<UsersService>()
        val messageTextProcessor = mock<MessageTextProcessor>()
        val callbackDataService = mock<CallbackDataService>()
        val creator = TelegramMessageCreator(usersService, callbackDataService, messageTextProcessor)
        val operatorBot = RecordingOperatorBot()
        val textMarkup = SendMessageMarkup("plain.text")

        whenever(messageTextProcessor.prepareText("plain.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, Any?>()))
            .thenReturn("plain.text")
        whenever(usersService.getUserChat("bot", 42L)).thenReturn(null)
        whenever(usersService.getUserChat("bot", 43L)).thenReturn(null)
        whenever(applicationContext.getBean("bot", OperatorBot::class.java)).thenReturn(operatorBot)

        val sender = TelegramMessageSender(applicationContext, usersService, messageTextProcessor, creator)

        val result =
            sender.sendMessages(
                MessageSender.SendMessageContext
                    .builder()
                    .botUsername("bot")
                    .chatId(listOf(42L, 43L))
                    .messageToSend(textMarkup)
                    .build(),
            )

        assertThat(result).hasSize(2)
        assertThat(operatorBot.calls).containsExactly("text:42", "text:43")
    }

    @Test
    fun `sendMessages fans out text photo and location in stable order for each chat`() {
        val applicationContext = mock<ApplicationContext>()
        val usersService = mock<UsersService>()
        val messageTextProcessor = mock<MessageTextProcessor>()
        val callbackDataService = mock<CallbackDataService>()
        val creator = TelegramMessageCreator(usersService, callbackDataService, messageTextProcessor)
        val operatorBot = RecordingOperatorBot()
        val textMarkup = SendMessageMarkup("plain.text")
        val photoMarkup =
            SendMessageMarkup
                .create("photo.text")
                .photo(SendMessageMarkup.Photo.ProviderFileId("telegram-file-id"))
                .build()

        whenever(messageTextProcessor.prepareText("plain.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, Any?>()))
            .thenReturn("plain.text")
        whenever(messageTextProcessor.prepareText("photo.text", MessageTextProcessor.DEFAULT_LOCALE, emptyMap<String, Any?>()))
            .thenReturn("photo.text")
        whenever(usersService.getUserChat("bot", 42L)).thenReturn(null)
        whenever(usersService.getUserChat("bot", 43L)).thenReturn(null)
        whenever(applicationContext.getBean("bot", OperatorBot::class.java)).thenReturn(operatorBot)

        val sender = TelegramMessageSender(applicationContext, usersService, messageTextProcessor, creator)

        val result =
            sender.sendMessages(
                MessageSender.SendMessageContext
                    .builder()
                    .botUsername("bot")
                    .chatId(listOf(42L, 43L))
                    .messagesToSend(listOf(textMarkup, photoMarkup))
                    .locationsToSend(listOf(SendLocationItem(1.0, 2.0)))
                    .build(),
            )

        assertThat(result).hasSize(6)
        assertThat(operatorBot.calls)
            .containsExactly(
                "text:42",
                "photo:42",
                "location:42",
                "text:43",
                "photo:43",
                "location:43",
            )
    }

    private class RecordingOperatorBot : OperatorBot {
        val calls = mutableListOf<String>()

        override val botUsername: String = "bot"
        override val updatesConsumer: LongPollingUpdateConsumer = LongPollingUpdateConsumer { }

        override val isRunning: Boolean = true

        override fun <T : Serializable, Method : BotApiMethod<T>> execute(method: Method): T {
            @Suppress("UNCHECKED_CAST")
            return when (method) {
                is SendMessage -> {
                    calls += "text:${method.chatId}"
                    Message().apply {
                        chat = Chat(method.chatId.toLong(), "private")
                        messageId = calls.size
                    } as T
                }

                is SendLocation -> {
                    calls += "location:${method.chatId}"
                    Message().apply {
                        chat = Chat(method.chatId.toLong(), "private")
                        messageId = calls.size
                    } as T
                }

                else -> error("Unsupported method $method")
            }
        }

        override fun execute(method: SendPhoto): Message {
            calls += "photo:${method.chatId}"
            return Message().apply {
                chat = Chat(method.chatId.toLong(), "private")
                messageId = calls.size
            }
        }

        override fun afterRegistration(botSession: BotSession) = Unit
    }
}
