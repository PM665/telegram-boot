package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.ports.BotActionContextProvider
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

class BotActionExecutor(
    val bean: Any,
    val beanMethod: Method,
    val botCommand: BotCommand,
    val botAction: BotAction,
    val contextResolver: BotActionContextProvider<*>,
) {
    private val log = LoggerFactory.getLogger(BotActionExecutor::class.java)

    @Suppress("TooGenericExceptionCaught")
    fun execute(
        botUsername: String,
        payloadData: PayloadData,
    ): ActionResult =
        try {
            val context = contextResolver.resolve(botUsername, payloadData)
            beanMethod.isAccessible = true
            val actionResult = beanMethod.invoke(bean, context) as ActionResult
            actionResult.putMessageArgs(context.baseMessageArgs)
            actionResult
        } catch (e: ReflectiveOperationException) {
            log.error("Error while invoking method: {}", beanMethod, e)
            ActionResult
                .create(ReplyStatus.error)
                .actionId("BotActionExecutor")
                .messagesToSend(listOf(SendMessageMarkup(e.message ?: e.javaClass.simpleName)))
                .build()
        } catch (_: RuntimeException) {
            // preserve original catch behavior
            contextResolver.errorResult(botUsername, botCommand.name)
        }
}
