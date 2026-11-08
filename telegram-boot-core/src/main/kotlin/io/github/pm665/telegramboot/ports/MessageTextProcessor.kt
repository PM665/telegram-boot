package io.github.pm665.telegramboot.ports

interface MessageTextProcessor {
    fun prepareText(
        text: String,
        locale: String?,
        messageArgs: Map<String, *>,
    ): String

    fun prepareText(
        text: String,
        locale: String?,
    ): String = prepareText(text, locale, emptyMap<String, Any?>())

    fun prepareText(
        text: String,
        messageArgs: Map<String, *>,
    ): String = prepareText(text, DEFAULT_LOCALE, messageArgs)

    fun prepareText(text: String): String = prepareText(text, DEFAULT_LOCALE, emptyMap<String, Any?>())

    companion object {
        val RU: String = "ru"

        val DEFAULT_LOCALE: String = RU

        val EN: String = "en"

        val SUPPORTED_LOCALES: Set<String> = setOf(RU, EN)
    }
}
