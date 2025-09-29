package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.Role
import io.github.pm665.telegramboot.domain.telegram.UserRole

interface UserRoleProvider {
    fun getUserRoles(): Collection<UserRole>

    fun getForBot(botUsername: String): Collection<UserRole>

    fun addUserRole(userRole: UserRole)

    fun removeUserRole(botUsername: String?, botUserId: Long, role: Role)
}
