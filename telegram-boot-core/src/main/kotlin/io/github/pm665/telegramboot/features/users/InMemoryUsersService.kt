package io.github.pm665.telegramboot.features.users

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.ports.UsersService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryUsersService : UsersService {
    private val users: MutableMap<BotUserKey, UsersService.User> = ConcurrentHashMap()
    private val chats: MutableMap<BotChatId, UsersService.Chat> = ConcurrentHashMap()
    private val userIdSequence = AtomicLong()

    override fun getUser(
        botUsername: String,
        userId: Long,
    ): UsersService.User? = users[BotUserKey(botUsername, userId)]

    override fun getUserByBotUserId(botUserId: Long): UsersService.User? = users.values.firstOrNull { it.botUserId == botUserId }

    override fun getUsers(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<UsersService.User> = userIds.mapNotNull { getUser(botUsername, it) }

    override fun findUserByUsername(
        botUsername: String,
        username: String,
    ): UsersService.User? =
        users.values.firstOrNull { user ->
            user.botUsername == botUsername && user.username?.equals(username, ignoreCase = true) == true
        }

    override fun getChat(
        botUsername: String,
        chatId: Long,
    ): UsersService.Chat? = chats[BotChatId(botUsername, chatId)]

    override fun getChats(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<UsersService.Chat> =
        chats.values
            .filter { chat -> chat.botUsername == botUsername }
            .filter { chat -> chat.enabled }
            .filter { chat -> userIds.contains(chat.userId) }
            .toList()

    override fun getChatsByBotUserIds(
        botUsername: String,
        botUserIds: Collection<Long>,
    ): Collection<UsersService.Chat> =
        chats.values
            .filter { chat -> chat.botUsername == botUsername && chat.enabled && chat.botUserId in botUserIds }
            .toList()

    override fun getChats(
        botUsername: String,
        accessType: AccessType,
    ): Collection<UsersService.Chat> =
        chats.values
            .filter { chat -> chat.botUsername == botUsername }
            .filter { chat -> chat.enabled }
            .toList()

    override fun createNewUser(
        botUsername: String,
        userId: Long,
        username: String?,
        locale: String,
    ): UsersService.User {
        val key = BotUserKey(botUsername, userId)
        val user = UsersService.User(userId, username, locale, userIdSequence.incrementAndGet(), botUsername)
        users[key] = user
        return user
    }

    override fun createNewChat(
        userId: Long,
        botUsername: String,
        chatId: Long,
    ): UsersService.Chat {
        val chat = UsersService.Chat(
            userId = userId,
            botUsername = botUsername,
            chatId = chatId,
            enabled = true,
            botUserId = getUser(botUsername, userId)?.botUserId ?: 0,
        )
        chats[BotChatId(botUsername, chatId)] = chat
        return chat
    }

    override fun setChatEnabled(
        botUsername: String,
        chatId: Long,
        enabled: Boolean,
    ) {
        val key = BotChatId(botUsername, chatId)
        chats.computeIfPresent(key) { _, chat -> chat.copy(enabled = enabled) }
    }

    data class BotUserKey(
        val botUsername: String,
        val userId: Long,
    )

    data class BotChatId(
        val botUsername: String,
        val chatId: Long,
    )
}
