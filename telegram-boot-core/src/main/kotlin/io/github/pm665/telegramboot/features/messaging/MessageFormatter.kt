package io.github.pm665.telegramboot.features.messaging

import com.samskivert.mustache.Mustache
import org.springframework.stereotype.Service

@Service
class MessageFormatter {
    fun formatMessage(
        text: String,
        messageArgs: Map<String, Any?>,
    ): String {
        val tmpl = Mustache.compiler().defaultValue("[ - ]").compile(text)
        return tmpl.execute(messageArgs)
    }
}
