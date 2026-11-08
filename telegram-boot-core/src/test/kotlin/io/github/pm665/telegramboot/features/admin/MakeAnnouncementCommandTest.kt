package io.github.pm665.telegramboot.features.admin

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotMessage
import io.github.pm665.telegramboot.domain.bot.BotMessageEntity
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MakeAnnouncementCommandTest {
    @Test
    fun `receiveBroadcastMessage saves formatted markdown text`() {
        val broadcastService = CapturingBroadcastMessagesService()
        val command =
            MakeAnnouncementCommand(
                broadcastMessagesService = broadcastService,
                messageSender = NoopMessageSender(),
                usersService = NoopUsersService(),
            )
        val update =
            BotUpdate(
                message =
                    BotIncomingMessage(
                        messageId = "77",
                        text = "Hi bold",
                        chat = BotChatInfo(42L, BotChatType.PRIVATE),
                        entities = listOf(BotMessageEntity("bold", 3, 4)),
                    ),
            )
        val context =
            BotActionContext(
                botUsername = "bot",
                payloadData = ChatUtils.createPayloadData(update),
                user =
                    UsersService.UserChat(
                        user = UsersService.User(10L, "admin", "en"),
                        chat = UsersService.Chat(10L, "bot", 42L, true),
                    ),
            )

        val result = command.receiveBroadcastMessage(context)

        assertThat(result.status).isEqualTo(ReplyStatus.ok)
        assertThat(result.messagesToSend).hasSize(1)
        assertThat(result.messagesToSend.first().text).isEqualTo("broadcast.confirm")
        assertThat(result.messagesToSend.first().replyToMessageId).isEqualTo("77")
        assertThat(broadcastService.saved).hasSize(1)
        assertThat(broadcastService.saved.first().message).isEqualTo("Hi *bold*")
    }

    private class CapturingBroadcastMessagesService : BroadcastMessagesService {
        val saved = mutableListOf<BroadcastMessage>()

        override fun save(broadcastMessage: BroadcastMessage): String {
            saved += broadcastMessage
            return "id-1"
        }

        override fun remove(id: String): Boolean = true

        override fun findById(id: String): BroadcastMessage? = null
    }

    private class NoopMessageSender : MessageSender {
        override fun sendMessage(
            sendMessageMarkup: SendMessageMarkup,
            chatId: Long,
            botUsername: String,
            messageArgs: Map<String, String>,
        ) = null

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

        override fun sendMessages(context: MessageSender.SendMessageContext) = emptyList<BotMessage>()

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

    private class NoopUsersService : UsersService {
        override fun getUser(
            botUsername: String,
            userId: Long,
        ): UsersService.User? = null

        override fun getUsers(
            botUsername: String,
            userIds: Collection<Long>,
        ): Collection<UsersService.User> = emptyList()

        override fun findUserByUsername(
            botUsername: String,
            username: String,
        ): UsersService.User? = null

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
        ): UsersService.User = UsersService.User(userId, username, locale, botUsername = botUsername)

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
