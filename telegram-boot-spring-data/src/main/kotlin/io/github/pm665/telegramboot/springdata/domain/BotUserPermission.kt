package io.github.pm665.telegramboot.springdata.domain

import io.github.pm665.telegramboot.domain.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("bot_user_permissions")
data class BotUserPermission(
    @Id
    var id: Long? = null,
    var userId: Long = 0,
    var botUsername: String? = null,
    var accessType: AccessType? = null,
    var approved: Boolean = false,
    var code: String? = null,
)
