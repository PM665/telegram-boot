package io.github.pm665.telegramboot.domain.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Confirmation(
    val message: String = "",
    val callbacks: Array<CallbackOption> = emptyArray(),
)
