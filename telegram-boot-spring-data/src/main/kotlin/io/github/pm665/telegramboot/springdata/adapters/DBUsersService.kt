package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.ports.UsersService
import io.github.pm665.telegramboot.springdata.domain.BotChat
import io.github.pm665.telegramboot.springdata.domain.BotUser
import io.github.pm665.telegramboot.springdata.ports.ChatsRepository
import io.github.pm665.telegramboot.springdata.ports.UsersRepository

class DBUsersService(
    private val usersRepository: UsersRepository,
    private val chatsRepository: ChatsRepository,
) : UsersService {
    override fun getOrCreateUser(
        botUsername: String,
        userId: Long,
        username: String?,
        locale: String,
    ): UsersService.User {
        val user = super.getOrCreateUser(botUsername, userId, username, locale)
        if (!username.isNullOrBlank() && username != user.username) {
            updateUser(user.copy(username = username, locale = locale))
        }
        return user
    }

    override fun getUser(
        botUsername: String,
        userId: Long,
    ): UsersService.User? = usersRepository.findByBotUsernameAndUserId(botUsername, userId)?.toDomain()

    override fun getUserByBotUserId(botUserId: Long): UsersService.User? =
        usersRepository.findById(botUserId).orElse(null)?.toDomain()

    override fun getUsers(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<UsersService.User> =
        usersRepository.findAllByBotUsernameAndUserIdIn(botUsername, userIds.toList()).map { it.toDomain() }.toList()

    override fun findUserByUsername(
        botUsername: String,
        username: String,
    ): UsersService.User? =
        usersRepository
            .findFirstByBotUsernameAndUsernameIgnoreCase(botUsername, username)
            ?.toDomain()

    override fun getChat(
        botUsername: String,
        chatId: Long,
    ): UsersService.Chat? =
        chatsRepository
            .findByBotUsernameAndChatId(botUsername, chatId)
            ?.let { chat ->
                usersRepository.findByBotUsernameAndUserId(botUsername, chat.userId)?.id?.let(chat::toDomain)
            }

    override fun getChats(
        botUsername: String,
        userIds: Collection<Long>,
    ): Collection<UsersService.Chat> =
        chatsRepository
            .findAllByBotUsernameAndUserIdInAndEnabledTrue(botUsername, userIds.toList())
            .mapNotNull { chat ->
                usersRepository.findByBotUsernameAndUserId(botUsername, chat.userId)?.id?.let(chat::toDomain)
            }
            .toList()

    override fun getChatsByBotUserIds(
        botUsername: String,
        botUserIds: Collection<Long>,
    ): Collection<UsersService.Chat> {
        if (botUserIds.isEmpty()) return emptyList()
        val userIds =
            usersRepository
                .findAllById(botUserIds)
                .filter { it.botUsername == botUsername }
                .map { it.userId }
        return getChats(botUsername, userIds)
    }

    override fun getChats(
        botUsername: String,
        accessType: AccessType,
    ): Collection<UsersService.Chat> =
        chatsRepository
            .findAllByBotUsernameAndAccessType(botUsername, accessType)
            .mapNotNull { chat ->
                usersRepository.findByBotUsernameAndUserId(botUsername, chat.userId)?.id?.let(chat::toDomain)
            }
            .toList()

    override fun createNewUser(
        botUsername: String,
        userId: Long,
        username: String?,
        locale: String,
    ): UsersService.User =
        BotUser(
            id = null,
            botUsername = botUsername,
            userId = userId,
            username = username,
            locale = locale,
            enabled = true,
        ).let { usersRepository.save(it) }.toDomain()

    override fun createNewChat(
        userId: Long,
        botUsername: String,
        chatId: Long,
    ): UsersService.Chat {
        val botUserId =
            usersRepository.findByBotUsernameAndUserId(botUsername, userId)?.id
                ?: throw IllegalStateException("Bot user not found: bot=$botUsername, userId=$userId")
        return BotChat(
            id = null,
            chatId = chatId,
            botUsername = botUsername,
            userId = userId,
            enabled = true,
        ).let { chatsRepository.save(it) }.toDomain(botUserId)
    }

    fun updateUser(user: UsersService.User) {
        usersRepository
            .findByBotUsernameAndUserId(requireNotNull(user.botUsername), user.userId)
            ?.apply {
                this.username = user.username
                this.locale = user.locale
                enabled = true
            }?.also { usersRepository.save(it) }
    }

    override fun setChatEnabled(
        botUsername: String,
        chatId: Long,
        enabled: Boolean,
    ) {
        chatsRepository
            .findByBotUsernameAndChatId(botUsername, chatId)
            ?.takeIf {
                it.enabled != enabled
            }?.apply { this.enabled = enabled }
            ?.also { chatsRepository.save(it) }
    }
}
