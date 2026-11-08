package io.github.pm665.telegramboot.features.messaging

import io.github.pm665.telegramboot.ports.MessageTextProcessor
import org.springframework.stereotype.Service

@Service
class MustacheMessageTextProcessor(
    private val messageFormatter: MessageFormatter,
    private val localizationService: LocalizationService,
) : MessageTextProcessor {
    override fun prepareText(
        text: String,
        locale: String?,
        messageArgs: Map<String, Any?>,
    ): String {
        val resolvedMessage = localizationService.resolveMessage(text, locale)
        val resolvedArgs = messageArgs.entries.associate { e -> e.key to resolveArg(locale, messageArgs, e) }
        return messageFormatter.formatMessage(resolvedMessage, resolvedArgs)
    }

    private fun resolveArg(
        locale: String?,
        messageArgs: Map<String, Any?>,
        e: Map.Entry<String, Any?>,
    ): Any? {
        val value = e.value
        return if (value is String) {
            val resolvedArg = localizationService.resolveMessage(value, locale)
            messageFormatter.formatMessage(resolvedArg, messageArgs)
        } else {
            value
        }
    }
}
