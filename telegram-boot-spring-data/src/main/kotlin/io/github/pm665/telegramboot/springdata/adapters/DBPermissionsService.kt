package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.PermissionsServiceAdapter
import io.github.pm665.telegramboot.ports.UsersService
import io.github.pm665.telegramboot.ports.UsersService.Chat
import io.github.pm665.telegramboot.ports.UsersService.User
import io.github.pm665.telegramboot.ports.UsersService.UserChat
import io.github.pm665.telegramboot.springdata.domain.BotUserPermission
import io.github.pm665.telegramboot.springdata.ports.BotUserPermissionsRepository
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64

open class DBPermissionsService(
    private val permissionsRepository: BotUserPermissionsRepository,
    private val usersService: UsersService,
    private val permissionsServiceAdapter: PermissionsServiceAdapter? = null,
    private val botsService: BotsService? = null,
) : PermissionsService {
    override fun checkUserAccessToCommand(
        botUsername: String,
        userChat: UserChat,
        command: BotCommand,
    ): Boolean = permissionsServiceAdapter?.checkUserAccessToCommand(userChat, command, this) ?: true

    override fun getUserAccess(
        botUsername: String,
        user: User,
    ): AccessType =
        permissionsRepository
            .findFirstByUserIdAndBotUsername(user.userId, botUsername)
            ?.accessType ?: AccessType.ANONYMOUS

    override fun isUserBotAdmin(
        user: User,
        botUsername: String,
    ): Boolean =
        permissionsRepository.existsByUserIdAndBotUsernameAndAccessTypeAndApprovedTrue(
            user.userId,
            botUsername,
            AccessType.BOT_ADMIN,
        )

    override fun isUserServiceAdmin(user: User): Boolean =
        user.botUsername?.let(::allowsGlobalAccess) != false &&
            permissionsRepository.existsByUserIdAndAccessTypeAndApprovedTrue(user.userId, AccessType.SERVICE_ADMIN)

    override fun isUserCreator(user: User): Boolean =
        user.botUsername?.let(::allowsGlobalAccess) != false &&
            permissionsRepository.existsByUserIdAndAccessTypeAndApprovedTrue(user.userId, AccessType.CREATOR)

    override fun findUsers(botUsername: String): Collection<Chat> {
        val chats = mutableListOf<Chat>()
        usersService.getChats(botUsername, AccessType.USER).forEach { chat -> chats.add(chat) }
        return chats
    }

    override fun findBotAdmins(botUsername: String): Collection<Chat> {
        val adminIds =
            permissionsRepository
                .findAllByBotUsernameAndAccessType(botUsername, AccessType.BOT_ADMIN)
                .filter { it.approved == true }
                .map { it.userId }
                .toSet()
        val chats = mutableListOf<Chat>()
        usersService.getChats(botUsername, adminIds.toMutableList()).forEach { chat -> chats.add(chat) }
        return chats
    }

    override fun findServiceAdmins(botUsername: String): Collection<Chat> {
        if (!allowsGlobalAccess(botUsername)) return emptyList()
        val serviceAdminIds =
            permissionsRepository
                .findAllByAccessType(AccessType.SERVICE_ADMIN)
                .filter { it.approved }
                .map { it.userId }
                .toSet()
        val chats = mutableListOf<Chat>()
        usersService.getChats(botUsername, serviceAdminIds.toMutableList()).forEach { chat -> chats.add(chat) }
        return chats
    }

    override fun findCreators(botUsername: String): Collection<Chat> {
        if (!allowsGlobalAccess(botUsername)) return emptyList()
        val creatorIds =
            permissionsRepository
                .findAllByAccessType(AccessType.CREATOR)
                .map { it.userId }
                .toSet()
        val chats = mutableListOf<Chat>()
        usersService.getChats(botUsername, creatorIds.toMutableList()).forEach { chat -> chats.add(chat) }
        return chats
    }

    override fun findServiceAdminUserIds(): Collection<Long> =
        permissionsRepository
            .findAllByAccessType(AccessType.SERVICE_ADMIN)
            .filter { it.approved == true }
            .map { it.userId }
            .toMutableSet()

    override fun findBotAdminUserIds(botUsername: String): Collection<Long> =
        permissionsRepository
            .findAllByBotUsernameAndAccessType(botUsername, AccessType.BOT_ADMIN)
            .filter { it.approved == true }
            .map { it.userId }
            .toMutableSet()

    @Transactional
    override fun setUserAccess(
        user: User,
        accessType: AccessType,
    ) {
        canBeAppliedToServiceLevel(accessType)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, null, accessType)
        if (permission != null) {
            permission.approved = true
            permissionsRepository.save(permission)
        } else {
            permissionsRepository.save(
                BotUserPermission(
                    userId = user.userId,
                    accessType = accessType,
                    approved = true,
                ),
            )
        }
    }

    override fun setUserAccess(
        user: User,
        botUsername: String,
        accessType: AccessType,
    ) {
        canBeAppliedToBotLevel(accessType)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, botUsername, accessType)
        if (permission != null) {
            permission.accessType = accessType
            permission.approved = true
            permissionsRepository.save(permission)
        } else {
            permissionsRepository.save(
                BotUserPermission(
                    userId = user.userId,
                    botUsername = botUsername,
                    accessType = accessType,
                    approved = true,
                ),
            )
        }
    }

    override fun removeServiceAdmin(userId: Long) {
        permissionsRepository
            .findFirstByUserIdAndBotUsernameAndAccessType(userId, null, AccessType.SERVICE_ADMIN)
            ?.let { permission -> permissionsRepository.delete(permission) }
    }

    override fun removeBotAdmin(
        user: User,
        botUsername: String,
    ) {
        permissionsRepository
            .findFirstByUserIdAndBotUsernameAndAccessType(user.userId, botUsername, AccessType.BOT_ADMIN)
            ?.let { permission -> permissionsRepository.delete(permission) }
    }

    override fun validateApproveCodeForUser(
        user: User,
        accessType: AccessType,
        inputCode: String,
    ): Boolean {
        canBeAppliedToServiceLevel(accessType)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, null, accessType)
        val stored = permission?.code
        return stored?.let { matches(inputCode, it) } ?: false
    }

    override fun validateApproveCodeForUser(
        user: User,
        botUsername: String,
        accessType: AccessType,
        inputCode: String,
    ): Boolean {
        canBeAppliedToBotLevel(accessType)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, botUsername, accessType)
        val stored = permission?.code
        return stored?.let { matches(inputCode, it) } ?: false
    }

    override fun saveApproveCodeForUser(
        user: User,
        accessType: AccessType,
        approveCode: String,
    ) {
        canBeAppliedToServiceLevel(accessType)
        val encryptedCode = encode(approveCode)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, null, accessType)
        if (permission != null) {
            permission.code = encryptedCode
            permissionsRepository.save(permission)
        } else {
            permissionsRepository.save(
                BotUserPermission(
                    userId = user.userId,
                    accessType = accessType,
                    code = encryptedCode,
                ),
            )
        }
    }

    override fun saveApproveCodeForUser(
        user: User,
        botUsername: String,
        accessType: AccessType,
        approveCode: String,
    ) {
        canBeAppliedToBotLevel(accessType)
        val encryptedCode = encode(approveCode)
        val permission =
            permissionsRepository.findFirstByUserIdAndBotUsernameAndAccessType(user.userId, botUsername, accessType)
        if (permission != null) {
            permission.code = encryptedCode
            permissionsRepository.save(permission)
        } else {
            permissionsRepository.save(
                BotUserPermission(
                    userId = user.userId,
                    botUsername = botUsername,
                    accessType = accessType,
                    code = encryptedCode,
                ),
            )
        }
    }

    companion object {
        private val SECURE_RANDOM = SecureRandom()
        private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()
        private val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()
        private const val HASH_ALGORITHM = "SHA-256"
        private const val SALT_LENGTH = 16

        private fun encode(rawValue: String): String {
            val salt = ByteArray(SALT_LENGTH)
            SECURE_RANDOM.nextBytes(salt)
            val hash = hash(rawValue, salt)
            return BASE64_ENCODER.encodeToString(salt) + ":" + BASE64_ENCODER.encodeToString(hash)
        }

        private fun matches(
            rawValue: String,
            storedValue: String?,
        ): Boolean {
            if (storedValue.isNullOrBlank()) {
                return false
            }
            val parts = storedValue.split(":", limit = 2)
            if (parts.size != 2) {
                return false
            }
            return try {
                val salt = BASE64_DECODER.decode(parts[0])
                val expected = BASE64_DECODER.decode(parts[1])
                val actual = hash(rawValue, salt)
                MessageDigest.isEqual(expected, actual)
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        private fun hash(
            rawValue: String,
            salt: ByteArray,
        ): ByteArray =
            try {
                val digest = MessageDigest.getInstance(HASH_ALGORITHM)
                digest.update(salt)
                digest.digest(rawValue.toByteArray(StandardCharsets.UTF_8))
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException("$HASH_ALGORITHM algorithm not available", e)
            }
    }

    private fun allowsGlobalAccess(botUsername: String): Boolean =
        botsService?.findBotByBotUsername(botUsername)?.platform != BotPlatform.MAX
}
