package io.github.pm665.telegramboot.domain.annotations

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.bot.BotFeature
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BotCommand(
    val name: String,
    val description: String = "",
    val displayName: String = "",
    val accessType: AccessType = AccessType.USER,
    val visibility: Visibility = Visibility.VISIBLE,
    val contextClass: KClass<out BotActionContext> = BotActionContext::class,
    val maxDelaySeconds: Long = 0,
    val keepMessage: Boolean = false,
    val features: Array<BotFeature> = [],
    val row: Long = 0,
    val order: Long = 0,
    val questionsChannel: String = "", // DO NOT ADD TO ANY OTHER COMMANDS EXCEPT ASK QUESTIONS COMMAND!
) {
    companion object {
        const val NO_COMMAND: String = "no-command"

        const val COMMAND_START: String = "start"

        const val IMPORTANT_PARK_LINKS_ROW: Long = 100

        const val IMPORTANT_COMMON_LINKS_ROW: Long = 200

        const val BASE_ROW: Long = 500

        const val MAX_BASE_ROW: Long = 1499

        const val PARK_LINKS_ROW: Long = 1700

        const val COMMON_LINKS_ROW: Long = 1800

        const val ADMIN_ROW: Long = 1900

        const val SETTINGS_ROW: Long = 2000
    }
}
