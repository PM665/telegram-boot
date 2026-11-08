package io.github.pm665.telegramboot.features.permissions

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.springframework.security.crypto.bcrypt.BCrypt
import java.util.concurrent.ConcurrentHashMap

class InMemoryPermissionsService(
    private val usersService: UsersService,
    private val botsService: BotsService? = null,
) : PermissionsService {
    companion object {
        const val SERVICE_LEVEL = "#service_level#"
    }

    private val userRoles: MutableMap<UsersService.User, MutableMap<String, AccessType>> = ConcurrentHashMap()
    private val codes: MutableMap<String, String> = ConcurrentHashMap()

    override fun getUserAccess(
        botUsername: String,
        user: UsersService.User,
    ): AccessType = AccessType.ANONYMOUS

    override fun isUserBotAdmin(
        user: UsersService.User,
        botUsername: String,
    ): Boolean = userRoles.getOrDefault(user, mapOf())[botUsername]?.let { it == AccessType.BOT_ADMIN } ?: false

    override fun isUserServiceAdmin(user: UsersService.User): Boolean =
        allowsGlobalAccess(user) &&
            userRoles.getOrDefault(user, mapOf())[SERVICE_LEVEL]?.let { it == AccessType.SERVICE_ADMIN } == true

    override fun isUserCreator(user: UsersService.User): Boolean =
        allowsGlobalAccess(user) && userRoles.getOrDefault(user, mapOf())[SERVICE_LEVEL]?.let { it == AccessType.CREATOR } == true

    override fun findUsers(botUsername: String): Collection<UsersService.Chat> = usersService.getChats(botUsername, AccessType.USER)

    override fun findBotAdmins(botUsername: String): Collection<UsersService.Chat> {
        val permissions =
            userRoles.entries
                .filter { entry -> entry.value.getOrDefault(botUsername, AccessType.ANONYMOUS) == AccessType.BOT_ADMIN }
                .map { it.key }
                .map { it.userId }
        return usersService.getChats(botUsername, permissions)
    }

    override fun findServiceAdmins(botUsername: String): Collection<UsersService.Chat> {
        if (!allowsGlobalAccess(botUsername)) return emptyList()
        val permissions =
            userRoles.entries
                .filter { entry -> entry.value.getOrDefault(SERVICE_LEVEL, AccessType.ANONYMOUS) == AccessType.SERVICE_ADMIN }
                .map { it.key }
                .map { it.userId }
        return usersService.getChats(botUsername, permissions)
    }

    override fun findCreators(botUsername: String): Collection<UsersService.Chat> {
        if (!allowsGlobalAccess(botUsername)) return emptyList()
        val permissions =
            userRoles.entries
                .filter { entry -> entry.value.getOrDefault(SERVICE_LEVEL, AccessType.ANONYMOUS) == AccessType.CREATOR }
                .map { it.key }
                .map { it.userId }
        return usersService.getChats(botUsername, permissions)
    }

    override fun findServiceAdminUserIds(): Collection<Long> =
        userRoles.entries
            .filter { entry -> entry.value.getOrDefault(SERVICE_LEVEL, AccessType.ANONYMOUS) == AccessType.SERVICE_ADMIN }
            .map { it.key }
            .map { it.userId }

    override fun findBotAdminUserIds(botUsername: String): Collection<Long> =
        userRoles.entries
            .filter { entry -> entry.value.getOrDefault(botUsername, AccessType.ANONYMOUS) == AccessType.BOT_ADMIN }
            .map { it.key }
            .map { it.userId }

    override fun setUserAccess(
        user: UsersService.User,
        accessType: AccessType,
    ) {
        canBeAppliedToServiceLevel(accessType)
        userRoles.computeIfAbsent(user) { ConcurrentHashMap() }[SERVICE_LEVEL] = accessType
    }

    override fun setUserAccess(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
    ) {
        canBeAppliedToBotLevel(accessType)
        userRoles.computeIfAbsent(user) { ConcurrentHashMap() }[botUsername] = accessType
    }

    override fun removeServiceAdmin(userId: Long) {
        userRoles.entries.filter { it.key.userId == userId }.forEach { entry ->
            val roles = entry.value
            roles.remove(SERVICE_LEVEL, AccessType.SERVICE_ADMIN)
            if (roles.isEmpty()) userRoles.remove(entry.key)
        }
    }

    override fun removeBotAdmin(
        user: UsersService.User,
        botUsername: String,
    ) {
        userRoles.entries.find { entry -> entry.key.userId == user.userId }?.let { entry ->
            val roles = entry.value
            roles.remove(botUsername, AccessType.BOT_ADMIN)
            if (roles.isEmpty()) userRoles.remove(entry.key)
        }
    }

    override fun validateApproveCodeForUser(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
        inputCode: String,
    ): Boolean = BCrypt.checkpw(inputCode, codes.getOrDefault((user.username ?: "") + "|" + botUsername + "|" + accessType.name, ""))

    override fun validateApproveCodeForUser(
        user: UsersService.User,
        accessType: AccessType,
        inputCode: String,
    ): Boolean = BCrypt.checkpw(inputCode, codes.getOrDefault((user.username ?: "") + "|" + accessType.name, ""))

    override fun saveApproveCodeForUser(
        user: UsersService.User,
        accessType: AccessType,
        approveCode: String,
    ) {
        codes[(user.username ?: "") + "|" + accessType.name] = BCrypt.hashpw(approveCode, BCrypt.gensalt())
    }

    override fun saveApproveCodeForUser(
        user: UsersService.User,
        botUsername: String,
        accessType: AccessType,
        approveCode: String,
    ) {
        codes[(user.username ?: "") + "|" + botUsername + "|" + accessType.name] = BCrypt.hashpw(approveCode, BCrypt.gensalt())
    }

    private fun allowsGlobalAccess(user: UsersService.User): Boolean =
        user.botUsername?.let(::allowsGlobalAccess) != false

    private fun allowsGlobalAccess(botUsername: String): Boolean =
        botsService?.findBotByBotUsername(botUsername)?.platform != BotPlatform.MAX
}
