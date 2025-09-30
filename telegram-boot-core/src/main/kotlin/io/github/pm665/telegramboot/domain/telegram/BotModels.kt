package io.github.pm665.telegramboot.domain.telegram

data class Bot(
    val botUsername: String, // identifyiable by
    val botToken: String,
    val enabled: Boolean,
)

data class BotUser(
    val botUserId: Long, // identifyiable by
    val username: String,
    val active: Boolean = false,
)

data class BotChat(
    val botUsername: String, // identifyiable by
    val botUserId: Long, // identifyiable by
    val botChatId: Long,
)

data class UserRole(
    val botUsername: String?, // identifyiable by
    val botUserId: Long, // identifyiable by
    val role: Role,
)

enum class Role {
    SUPER_ADMIN,
    BOT_ADMIN,
    USER,
}

data class Command(
    val command: String, // identifyiable by
    val botUsername: String, // identifyiable by
    val enabled: Boolean, // identifyiable by
    val type: CommandType,
    val action: String,
    val label: String,
    val description: String,
)

data class Menu(
    val command: String, // identifyiable by
    val botUsername: String, // identifyiable by
    val enabled: Boolean, // identifyiable by
    val parent: String? = null,
    val section: Int = 0,
    val row: Int = 0,
    val order: Int = 0,
)

data class Message(
    val id: String, // identifyiable by
    val botUsername: String,
    val chatId: Long,
    val className: String,
    val update: Any,
)

enum class CommandType {
    COMMAND,
    INFO_LINK,
    EXTERNAL,
    MENU,
}
