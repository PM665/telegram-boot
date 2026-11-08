package io.github.pm665.telegramboot.drivers.telegram

import org.springframework.aop.Advisor
import org.springframework.aop.aspectj.AspectJProxyUtils
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.framework.autoproxy.BeanFactoryAdvisorRetrievalHelper
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.AnnotationAwareOrderComparator

open class AdvisorRegisteringClientRegistry<T : Any>(
    private val appContext: ConfigurableApplicationContext,
    private val clientClass: Class<T>,
    private val beanCreator: (String) -> T,
) {
    private val beanFactory get() = appContext.beanFactory
    private val advisorHelper by lazy { BeanFactoryAdvisorRetrievalHelper(beanFactory) }
    private val aspectJAdvisorBuilder by lazy { BeanFactoryAspectJAdvisorsBuilder(beanFactory) }

    fun getClient(identifier: String): T =
        try {
            appContext.getBean(beanName(identifier), clientClass)
        } catch (_: NoSuchBeanDefinitionException) {
            createClient(identifier)
        }

    private fun createClient(identifier: String): T {
        val beanName = beanName(identifier)
        val target = createBeanObject(identifier)
        val proxy = createProxiedClient(target)
        beanFactory.registerSingleton(beanName, proxy)
        return proxy
    }

    protected open fun createBeanObject(identifier: String): T = beanCreator(identifier)

    protected open fun beanName(identifier: String): String = "${beanNamePrefix()}[$identifier]"

    private fun beanNamePrefix(): String {
        val simpleName = clientClass.simpleName
        val withoutClientSuffix = simpleName.removeSuffix("Client")
        val withoutYandexPrefix = withoutClientSuffix.removePrefix("Yandex")
        val candidate =
            when {
                withoutYandexPrefix.isNotEmpty() -> withoutYandexPrefix + simpleName.substring(withoutClientSuffix.length)
                withoutClientSuffix.isNotEmpty() -> withoutClientSuffix + simpleName.substring(withoutClientSuffix.length)
                else -> simpleName
            }

        return candidate.replaceFirstChar { if (it.isUpperCase()) it.lowercaseChar() else it }
    }

    private fun createProxiedClient(target: T): T {
        val advisors = collectAdvisors(target)
        if (advisors.isEmpty()) {
            return target
        }

        val proxyFactory =
            ProxyFactory().apply {
                setTarget(target)
                isProxyTargetClass = !clientClass.isInterface
                if (clientClass.isInterface) {
                    addInterface(clientClass)
                }
                advisors.forEach { addAdvisor(it) }
            }

        @Suppress("UNCHECKED_CAST")
        return proxyFactory.getProxy(clientClass.classLoader) as T
    }

    private fun collectAdvisors(target: T): List<Advisor> {
        val advisors = advisorHelper.findAdvisorBeans().toMutableList()
        advisors += aspectJAdvisorBuilder.buildAspectJAdvisors()

        val eligible = advisors.filter { AopUtils.canApply(it, target::class.java) }.toMutableList()
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(eligible)
        AnnotationAwareOrderComparator.sort(eligible)
        return eligible
    }
}
