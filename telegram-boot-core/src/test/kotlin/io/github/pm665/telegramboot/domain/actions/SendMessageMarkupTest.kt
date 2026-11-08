package io.github.pm665.telegramboot.domain.actions

import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.MessageSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendMessageMarkupTest {
    @Test
    fun `message markup can carry uploaded photo source through send context`() {
        val photo = SendMessageMarkup.Photo.Bytes(filename = "banner.jpg", contentType = "image/jpeg", bytes = byteArrayOf(1, 2, 3))
        val markup = SendMessageMarkup(text = "announcement.text", photo = photo)

        val context =
            MessageSender.SendMessageContext
                .builder()
                .botUsername("bot")
                .chat(42L)
                .messageToSend(markup)
                .build()

        assertThat(context.messagesToSend).singleElement().isSameAs(markup)
        assertThat(context.messagesToSend.single().photo).isEqualTo(photo)
    }

    @Test
    fun `send text message keeps existing text only behavior`() {
        val sender = CapturingMessageSender()

        sender.sendTextMessage("hello", 42L, "bot")

        assertThat(sender.lastMarkup?.text).isEqualTo("hello")
        assertThat(sender.lastMarkup?.photo).isNull()
    }

    @Test
    fun `send context keeps text photo and location payloads together`() {
        val photoMarkup =
            SendMessageMarkup
                .create("announcement.text")
                .photo(SendMessageMarkup.Photo.ProviderFileId("file-id-1"))
                .build()
        val textMarkup = SendMessageMarkup("plain.text")
        val location = SendLocationItem(1.0, 2.0)

        val context =
            MessageSender.SendMessageContext
                .builder()
                .botUsername("bot")
                .chat(42L)
                .messagesToSend(listOf(textMarkup, photoMarkup))
                .locationsToSend(listOf(location))
                .build()

        assertThat(context.messagesToSend).containsExactly(textMarkup, photoMarkup)
        assertThat(context.locationsToSend).containsExactly(location)
    }

    private class CapturingMessageSender : MessageSender {
        var lastMarkup: SendMessageMarkup? = null

        override fun sendMessage(
            sendMessageMarkup: SendMessageMarkup,
            chatId: Long,
            botUsername: String,
            messageArgs: Map<String, String>,
        ): BotMessage? {
            lastMarkup = sendMessageMarkup
            return null
        }

        override fun sendLocation(
            lat: Double,
            lon: Double,
            chatId: Long,
            botUsername: String,
        ) = null

        override fun sendCallbackNotification(
            update: BotUpdate,
            text: String,
            messageArgs: Map<String, String>,
            botUsername: String,
        ) {}

        override fun deleteCallbackSource(
            update: BotUpdate?,
            botUsername: String,
        ) {}

        override fun deleteMessage(
            messageId: String?,
            chatId: Long,
            botUsername: String,
        ) {}

        override fun pinMessage(
            messageId: String?,
            chatId: Long,
            botUsername: String,
        ) {}

        override fun unpinMessage(
            messageId: String?,
            chatId: Long,
            botUsername: String,
        ) {}

        override fun sendMessages(context: MessageSender.SendMessageContext): Collection<BotMessage> = emptyList()

        override fun editMessage(
            botUsername: String,
            update: BotUpdate,
            text: String,
            inlineKeyboard: SendMessageMarkup.MenuMarkup?,
            messageArgs: Map<String, Any>,
        ) {}

        override fun sendCommands(
            botUsername: String,
            commands: Map<String, String>,
        ) {}
    }
}
