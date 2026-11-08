package io.github.pm665.telegramboot.gmail

interface EmailSender {
    fun send(
        recipient: String,
        subject: String,
        htmlTemplate: String,
        model: Map<String, Any?> = emptyMap(),
    )
}
