package io.github.pm665.telegramboot.adapters

import io.github.pm665.telegramboot.domain.telegram.Menu
import io.github.pm665.telegramboot.ports.MenuProvider
import java.util.concurrent.ConcurrentHashMap

class InMemoryMenuProvider : MenuProvider {
    private data class MenuKey(val command: String, val botUsername: String)

    private val menus = ConcurrentHashMap<MenuKey, Menu>()

    override fun getMenus(): Collection<Menu> = menus.values.toList()

    override fun getForBot(botUsername: String): Collection<Menu> = menus.values.filter { it.botUsername == botUsername }

    override fun getByParent(
        parent: String?,
        botUsername: String,
    ): Collection<Menu> = menus.values.filter { it.botUsername == botUsername && it.parent == parent }

    override fun addMenu(menu: Menu) {
        val key = MenuKey(menu.command, menu.botUsername)
        menus[key] = menu
    }

    override fun removeMenu(
        command: String,
        botUsername: String,
    ) {
        val key = MenuKey(command, botUsername)
        menus.remove(key)
    }
}
