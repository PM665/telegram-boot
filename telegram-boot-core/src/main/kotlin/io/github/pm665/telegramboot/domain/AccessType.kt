package io.github.pm665.telegramboot.domain

enum class AccessType(
    val startTextCode: String,
) {
    ALL("start.text"),
    USER("start.text.USER"),
    USER_AND_SERVICE_ADMIN("start.text.USER"),
    BOT_ADMIN("start.text.BOT_ADMIN"),
    SERVICE_ADMIN("start.text.BOT_ADMIN"),
    CREATOR("start.text.BOT_ADMIN"),
    ANONYMOUS("start.text.ANONYMOUS"),
}
