package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.BotActionExecutor
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.ports.BotActionContextProvider
import io.github.pm665.telegramboot.ports.BotCommandsProvider
import io.github.pm665.telegramboot.ports.BotFeatureSupport
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service
import java.lang.reflect.Method

@Service
class BotActionLocatorService(
    private val applicationContext: ApplicationContext,
    private val botFeatureSupport: BotFeatureSupport,
) : BotCommandsProvider {
    override fun findAllBotCommands(): List<BotCommand> {
        val commandBeans = applicationContext.getBeansWithAnnotation(BotCommand::class.java)
        return commandBeans.values
            .map {
                requireNotNull(AnnotationUtils.findAnnotation(it.javaClass, BotCommand::class.java)) {
                    "Bot command annotation is required"
                }
            }
            .toList()
    }

    fun findBotAction(
        botUsername: String,
        commandName: String,
        stateName: String,
        callbackName: String?,
    ): BotActionExecutor? {
        val commandBeans = applicationContext.getBeansWithAnnotation(BotCommand::class.java)
        return commandBeans.values
            .filter { bean -> filterAnnotatedBean(bean, botUsername, commandName.replaceFirst("^/".toRegex(), "")) }
            .map { bean -> createBotActionExecutor(bean, stateName, callbackName) }
            .firstOrNull()
    }

    private fun createBotActionExecutor(
        bean: Any,
        stateName: String,
        callbackName: String?,
    ): BotActionExecutor? {
        val botCommand = AnnotationUtils.findAnnotation(bean.javaClass, BotCommand::class.java)
        return bean.javaClass.methods
            .firstOrNull { method -> filterAnnotatedMethod(method, stateName, callbackName) }
            ?.let { method -> createBotActionExecutor(bean, method, botCommand) }
    }

    private fun createBotActionExecutor(
        bean: Any,
        method: Method,
        botCommand: BotCommand?,
    ): BotActionExecutor? {
        val botAction = AnnotationUtils.findAnnotation(method, BotAction::class.java) ?: return null
        val contextResolver = getBotActionContextResolver(requireNotNull(botCommand) { "Bot command annotation is required" }, botAction)
        return BotActionExecutor(bean, method, botCommand, botAction, contextResolver)
    }

    private fun filterAnnotatedBean(
        bean: Any,
        botUsername: String,
        commandName: String,
    ): Boolean {
        val botCommand = AnnotationUtils.findAnnotation(bean.javaClass, BotCommand::class.java) ?: return false
        return botCommand.name.equals(commandName, ignoreCase = true) &&
            botCommand.features.all { feature -> botFeatureSupport.isSupported(botUsername, feature) }
    }

    private fun filterAnnotatedMethod(
        method: Method,
        stateName: String,
        callbackName: String?,
    ): Boolean {
        val botAction = AnnotationUtils.findAnnotation(method, BotAction::class.java) ?: return false
        if (botAction.onState == stateName) {
            return callbackName.isNullOrBlank() ||
                botAction.onCallback.isBlank() ||
                callbackName == botAction.onCallback ||
                BotAction.ANY_CALLBACK == botAction.onCallback
        }
        if (stateName != botAction.onState + BotAction.CONFIRM_SUFFIX) {
            return false
        }
        return !callbackName.isNullOrBlank() &&
            (botAction.onCallback.isBlank() || callbackName == botAction.onCallback)
    }

    private fun getBotActionContextResolver(
        botCommand: BotCommand,
        botAction: BotAction,
    ): BotActionContextProvider<*> {
        var contextClass: Class<*> = botCommand.contextClass.java
        if (BotActionContext::class.java == contextClass) {
            contextClass = botAction.contextClass.java
        }
        val resolverType = ResolvableType.forClassWithGenerics(BotActionContextProvider::class.java, contextClass)
        val provider = applicationContext.getBeanProvider<BotActionContextProvider<*>>(resolverType)
        return provider.stream().findFirst().orElse(null) ?: throw NoSuchBeanDefinitionException(resolverType)
    }
}
