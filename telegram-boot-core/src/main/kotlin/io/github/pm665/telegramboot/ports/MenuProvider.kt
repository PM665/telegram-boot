package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.telegram.Menu

interface MenuProvider {
    fun getMenus(): Collection<Menu>

    fun getForBot(botUsername: String): Collection<Menu>

    fun getByParent(
        parent: String?,
        botUsername: String,
    ): Collection<Menu>

    fun addMenu(menu: Menu)

    fun removeMenu(
        command: String,
        botUsername: String,
    )
}
