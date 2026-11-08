package io.github.pm665.telegramboot.domain.actions

class ActionResult private constructor(
    val actionId: String,
    val status: String?,
    messageArgs: Map<String, Any?>,
    val messagesToSend: List<SendMessageMarkup>,
    val locationsToSend: List<SendLocationItem>,
) {
    var messageArgs: Map<String, Any?> = messageArgs
        private set

    fun putMessageArgs(contextMessageArgs: Map<String, Any>) {
        val updated = messageArgs.toMutableMap()
        updated.putAll(contextMessageArgs)
        messageArgs = updated
    }

    companion object {
        fun builder(): ActionResultBuilder = ActionResultBuilder()

        fun create(replyStatus: String): ActionResultBuilder = builder().status(replyStatus)
    }

    class ActionResultBuilder {
        private var actionId: String? = null
        private var status: String? = null
        private val messageArgs: MutableMap<String, Any?> = linkedMapOf()
        private val messagesToSend: MutableList<SendMessageMarkup> = mutableListOf()
        private val locationsToSend: MutableList<SendLocationItem> = mutableListOf()
        private var messagesToSendSet = false
        private var locationsToSendSet = false

        fun actionId(actionId: String) = apply { this.actionId = actionId }

        fun status(status: String?) = apply { this.status = status }

        fun messageArgs(messageArgs: Map<String, *>) =
            apply {
                for ((key, value) in messageArgs) {
                    this.messageArgs[key] = value
                }
            }

        fun messageArg(
            key: String,
            value: Any?,
        ) = apply { messageArgs[key] = value }

        fun messagesToSend(messagesToSend: List<SendMessageMarkup>) =
            apply {
                messagesToSendSet = true
                this.messagesToSend.clear()
                this.messagesToSend.addAll(messagesToSend)
            }

        fun locationsToSend(locationsToSend: List<SendLocationItem>) =
            apply {
                locationsToSendSet = true
                this.locationsToSend.clear()
                this.locationsToSend.addAll(locationsToSend)
            }

        fun build(): ActionResult =
            ActionResult(
                actionId = actionId ?: "no-action",
                status = status,
                messageArgs = messageArgs.toMap(),
                messagesToSend = if (messagesToSendSet) messagesToSend.toList() else emptyList(),
                locationsToSend = if (locationsToSendSet) locationsToSend.toList() else emptyList(),
            )
    }
}
