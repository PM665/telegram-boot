package io.github.pm665.telegramboot.domain.annotations

import io.github.pm665.telegramboot.domain.properties.ReplyStatus

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Reply(
    val onStatus: String = ReplyStatus.ok,
    val transitionToState: String = "",
    val messagesToSend: Array<String> = emptyArray(),
    val immediateTransition: Boolean = true,
)
