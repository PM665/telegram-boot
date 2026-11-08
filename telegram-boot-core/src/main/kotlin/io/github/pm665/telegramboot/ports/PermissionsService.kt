package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.annotations.BotCommand

interface PermissionsService {
    fun getUserAccess(
        botUsername: String,
        user: UsersService.User,
    ): AccessType

    fun isUserBotAdmin(
        user: UsersService.User,
        botUsername: String,
    ): Boolean

    fun isUserServiceAdmin(user: UsersService.User): Boolean

    fun isUserCreator(user: UsersService.User): Boolean

    fun checkUserAccessToCommand(
        botUsername: String,
        userChat: UsersService.UserChat,
        command: BotCommand,
    ): Boolean = true

    fun hasUserAccessToCommand(
        botUsername: String,
        userChat: UsersService.UserChat?,
        command: BotCommand,
    ): Boolean {
        val isBotAdmin = userChat?.user?.let { isUserBotAdmin(it, botUsername) } ?: false
        val isServiceAdmin = userChat?.user?.let { isUserServiceAdmin(it) } ?: false
        return when (val accessType = command.accessType) {
            AccessType.ALL -> {
                true
            }

            AccessType.USER, AccessType.ANONYMOUS -> {
                userChat?.let { checkUserAccessToCommand(botUsername, it, command) }
                    ?: (accessType == AccessType.ANONYMOUS)
            }

            AccessType.USER_AND_SERVICE_ADMIN -> {
                isServiceAdmin || userChat?.let { checkUserAccessToCommand(botUsername, it, command) } ?: false
            }

            AccessType.BOT_ADMIN -> {
                isServiceAdmin || isBotAdmin
            }

            AccessType.SERVICE_ADMIN -> {
                isServiceAdmin
            }

            AccessType.CREATOR -> {
                userChat?.user?.let { user -> isUserCreator(user) } ?: false
            }
        }
    }

    fun canBeAppliedToBotLevel(accessType: AccessType) {
        if (!botLevelAccessTypes.contains(accessType)) {
            throw IllegalArgumentException("Access type $accessType can't be applied to bot level")
        }
    }

    fun canBeAppliedToServiceLevel(accessType: AccessType) {
        if (!serviceLevelAccessTypes.contains(accessType)) {
            throw IllegalArgumentException("Access type $accessType can't be applied to service level")
        }
    }

    fun findUsers(botUsername: String): Collection<UsersService.Chat>

    fun findBotAdmins(botUsername: String): Collection<UsersService.Chat>

    fun findServiceAdmins(botUsername: String): Collection<UsersService.Chat>

    fun findCreators(botUsername: String): Collection<UsersService.Chat>

    fun findServiceAdminUserIds(): Collection<Long>

    fun findBotAdminUserIds(botUsername: String): Collection<Long>

    fun setUserAccess(
        user: UsersService.User,
        accessType: AccessType,
    )

    fun setUserAccess(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
    )

    fun removeServiceAdmin(userId: Long)

    fun removeBotAdmin(
        user: UsersService.User,
        botUsername: String,
    )

    fun validateApproveCodeForUser(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
        inputCode: String,
    ): Boolean

    fun validateApproveCodeForUser(
        user: UsersService.User,
        accessType: AccessType,
        inputCode: String,
    ): Boolean

    fun saveApproveCodeForUser(
        user: UsersService.User,
        accessType: AccessType,
        approveCode: String,
    )

    fun saveApproveCodeForUser(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
        approveCode: String,
    )

    companion object {
        val botLevelAccessTypes: Set<AccessType> =
            setOf(AccessType.BOT_ADMIN, AccessType.USER, AccessType.ANONYMOUS)

        val serviceLevelAccessTypes: Set<AccessType> =
            setOf(AccessType.SERVICE_ADMIN, AccessType.CREATOR)
    }
}
