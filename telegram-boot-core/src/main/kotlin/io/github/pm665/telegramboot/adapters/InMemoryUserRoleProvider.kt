package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Role
import io.github.pm665.telegramboot.domain.telegram.UserRole
import io.github.pm665.telegramboot.ports.UserRoleProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserRoleProvider : UserRoleProvider {
    private data class UserRoleKey(val botUsername: String?, val botUserId: Long, val role: Role)

    private val userRoles = ConcurrentHashMap<UserRoleKey, UserRole>()

    override fun getUserRoles(): Collection<UserRole> = userRoles.values.toList()

    override fun getForBot(botUsername: String): Collection<UserRole> = userRoles.values.filter { it.botUsername == botUsername }

    override fun addUserRole(userRole: UserRole) {
        val key = UserRoleKey(userRole.botUsername, userRole.botUserId, userRole.role)
        userRoles[key] = userRole
    }

    override fun removeUserRole(
        botUsername: String?,
        botUserId: Long,
        role: Role,
    ) {
        val key = UserRoleKey(botUsername, botUserId, role)
        userRoles.remove(key)
    }
}
