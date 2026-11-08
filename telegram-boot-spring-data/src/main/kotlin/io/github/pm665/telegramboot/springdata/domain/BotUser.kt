package io.github.pm665.telegramboot.springdata.domain

import io.github.pm665.telegramboot.ports.UsersService
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("bot_users")
data class BotUser(
    @Id
    var id: Long? = null,
    var botUsername: String? = null,
    var userId: Long = 0,
    var username: String? = null,
    var locale: String? = null,
    var enabled: Boolean = true,
) {
    fun toDomain(): UsersService.User =
        UsersService.User(
            userId = this.userId,
            username = this.username.orEmpty(),
            locale = this.locale.orEmpty(),
            botUserId = requireNotNull(this.id) { "Bot user ID is required" },
            botUsername = this.botUsername,
        )
}
