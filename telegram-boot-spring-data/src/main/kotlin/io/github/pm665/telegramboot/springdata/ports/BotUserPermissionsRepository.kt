package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.springdata.domain.BotUserPermission
import org.springframework.data.repository.ListCrudRepository

interface BotUserPermissionsRepository : ListCrudRepository<BotUserPermission, Long> {
    fun existsByUserIdAndBotUsernameAndAccessTypeAndApprovedTrue(
        userId: Long,
        botUsername: String?,
        accessType: AccessType,
    ): Boolean

    fun existsByUserIdAndAccessTypeAndApprovedTrue(
        userId: Long,
        accessType: AccessType,
    ): Boolean

    fun findFirstByUserIdAndBotUsernameAndAccessType(
        userId: Long,
        botUsername: String?,
        accessType: AccessType,
    ): BotUserPermission?

    fun findFirstByUserIdAndBotUsername(
        userId: Long,
        botUsername: String?,
    ): BotUserPermission?

    fun findAllByBotUsernameAndAccessType(
        botUsername: String,
        accessType: AccessType,
    ): List<BotUserPermission>

    fun findAllByAccessType(accessType: AccessType): List<BotUserPermission>
}
