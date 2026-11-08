package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.AccessType

interface UsersService {
    fun getUser(
        botUsername: String,
        userId: Long,
    ): User?

    fun getUserByBotUserId(botUserId: Long): User? = null

    fun getUsers(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<User>

    fun findUserByUsername(
        botUsername: String,
        username: String,
    ): User?

    fun getOrCreateUser(
        botUsername: String,
        userId: Long,
        username: String?,
        locale: String,
    ): User = getUser(botUsername, userId) ?: createNewUser(botUsername, userId, username, locale)

    fun getUserChat(
        botUsername: String,
        chatId: Long,
    ): UserChat? {
        val chat = getChat(botUsername, chatId) ?: return null
        val user = getUser(botUsername, chat.userId) ?: return null
        return UserChat(user, chat)
    }

    fun getOrCreateUserChat(
        userId: Long,
        username: String?,
        locale: String,
        botUsername: String,
        chatId: Long,
    ): UserChat {
        val existingChat = getUserChat(botUsername, chatId)
        if (existingChat != null) {
            if (!existingChat.chat.enabled) {
                enableChat(botUsername, chatId)
                return getUserChat(botUsername, chatId) ?: existingChat
            }
            return existingChat
        }
        val user = getOrCreateUser(botUsername, userId, username, locale)
        val chat = getOrCreateNewChat(userId, botUsername, chatId)
        return UserChat(user, chat)
    }

    fun getChat(
        botUsername: String,
        chatId: Long,
    ): Chat?

    fun getChats(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<Chat>

    fun getChatsByBotUserIds(
        botUsername: String,
        botUserIds: Collection<Long>,
    ): Collection<Chat>

    fun getOrCreateNewChat(
        userId: Long,
        botUsername: String,
        chatId: Long,
    ): Chat = getChat(botUsername, chatId) ?: createNewChat(userId, botUsername, chatId)

    fun getChats(
        botUsername: String,
        accessType: AccessType,
    ): Collection<Chat>

    fun createNewUser(
        botUsername: String,
        userId: Long,
        username: String?,
        locale: String,
    ): User

    fun createNewChat(
        userId: Long,
        botUsername: String,
        chatId: Long,
    ): Chat

    fun enableChat(
        botUsername: String,
        chatId: Long,
    ) {
        setChatEnabled(botUsername, chatId, true)
    }

    fun disableChat(
        botUsername: String,
        chatId: Long,
    ) {
        setChatEnabled(botUsername, chatId, false)
    }

    fun setChatEnabled(
        botUsername: String,
        chatId: Long,
        enabled: Boolean,
    )

    data class User(
        val userId: Long,
        val username: String?,
        val locale: String,
        val botUserId: Long = 0,
        val botUsername: String? = null,
    )

    data class UserChat(
        val user: User,
        val chat: Chat,
    )

    data class Chat(
        val userId: Long,
        val botUsername: String,
        val chatId: Long,
        val enabled: Boolean,
        val botUserId: Long = 0,
    )
}
