package io.github.pm665.telegramboot.domain.annotations

import io.github.pm665.telegramboot.domain.BotActionContext
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BotAction(
    val onState: String = INIT_STATE,
    val onCallback: String = "",
    val replies: Array<Reply> = emptyArray(),
    val confirmation: Confirmation = Confirmation(),
    val contextClass: KClass<out BotActionContext> = BotActionContext::class,
    val offState: Boolean = false,
) {
    companion object {
        const val CONFIRM_SUFFIX: String = "_confirm"
        const val INIT_STATE: String = "init"
        const val ANY_CALLBACK: String = "any-callback"
    }
}
