package io.github.pm665.telegramboot.infrastructure.metrics

import io.github.pm665.telegramboot.domain.properties.ReplyStatus

enum class CommandResultStatus(
    val value: String,
) {
    COMPLETED("completed"),
    FAILED("failed"),
    FORGOTTEN("forgotten"),
    SKIPPED("skipped"),
    ;

    companion object {
        private const val STATUS_NO_ACTIVE_ORDER = "noActiveOrder"
        private const val STATUS_NOT_IN_WAITING_STATE = "notInWaitingState"
        private const val STATUS_NO_LOCATION_PROVIDED = "noLocationProvided"

        private val SUCCESS_STATUSES = setOf(ReplyStatus.ok, ReplyStatus.wait)

        private val FAILURE_STATUSES =
            setOf(
                ReplyStatus.error,
                ReplyStatus.invalidInput,
                ReplyStatus.contextError,
            )

        private val SKIPPED_STATUSES =
            setOf(
                ReplyStatus.notFound,
                ReplyStatus.alreadyExists,
                ReplyStatus.cancel,
                STATUS_NO_ACTIVE_ORDER,
                STATUS_NOT_IN_WAITING_STATE,
                STATUS_NO_LOCATION_PROVIDED,
                ReplyStatus.inactive,
            )

        fun fromActionStatus(status: String?): CommandResultStatus {
            if (status.isNullOrBlank()) {
                return FAILED
            }
            return when {
                SUCCESS_STATUSES.contains(status) -> COMPLETED
                FAILURE_STATUSES.contains(status) -> FAILED
                SKIPPED_STATUSES.contains(status) -> SKIPPED
                else -> FAILED
            }
        }
    }
}
