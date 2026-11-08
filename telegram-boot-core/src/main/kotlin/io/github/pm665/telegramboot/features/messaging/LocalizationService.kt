package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.ports.MessageTextProcessor
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class LocalizationService(
    private val messageSource: MessageSource,
) {
    fun resolveMessage(
        msg: String,
        locale: String?,
    ): String {
        val result = messageSource.getMessage(msg, arrayOf(), msg, Locale.forLanguageTag(locale)) ?: msg
        if (msg == result) {
            return messageSource.getMessage(
                msg,
                arrayOf(),
                msg,
                Locale.forLanguageTag(MessageTextProcessor.DEFAULT_LOCALE),
            ) ?: msg
        }
        return result
    }
}
