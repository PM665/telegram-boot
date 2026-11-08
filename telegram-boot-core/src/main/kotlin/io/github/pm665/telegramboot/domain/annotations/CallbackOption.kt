package io.github.pm665.telegramboot.domain.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class CallbackOption(
    val code: String,
    val text: String,
)
