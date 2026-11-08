@file:Suppress("PropertyName")

package io.github.pm665.telegramboot.domain.properties

object ReplyStatus {
    const val ok = "ok"
    const val error = "error"
    const val alreadyExists = "alreadyExists"
    const val notFound = "notFound"
    const val cancel = "cancel"
    const val contextError = "illegalState"
    const val invalidInput = "invalidInput"
    const val wait = "wait"
    const val inactive = "inactive"
}
