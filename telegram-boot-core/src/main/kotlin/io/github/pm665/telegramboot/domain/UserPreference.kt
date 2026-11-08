package io.github.pm665.telegramboot.domain

data class UserPreference(
    val botUsername: String,
    val userId: Long,
    val preferenceName: String,
    var value: String? = null,
) {
    var stringValue: String?
        get() = value
        set(value) {
            this.value = value
        }

    var booleanValue: Boolean
        get() = value?.toBooleanStrictOrNull() ?: false
        set(value) {
            this.value = value.toString()
        }

    var longValue: Long?
        get() = value?.toLongOrNull()
        set(value) {
            this.value = value?.toString()
        }
}
