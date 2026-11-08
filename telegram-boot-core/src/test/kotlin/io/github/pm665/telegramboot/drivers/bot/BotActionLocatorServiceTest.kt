package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.bot.BotFeature
import io.github.pm665.telegramboot.ports.BotActionContextProvider
import io.github.pm665.telegramboot.ports.BotFeatureSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.support.GenericApplicationContext

class BotActionLocatorServiceTest {
    @Test
    fun `findBotAction ignores any-callback handlers from other states`() {
        val applicationContext =
            GenericApplicationContext().apply {
                registerBeanDefinition("testCommand", RootBeanDefinition(TestCommand::class.java))
                registerBeanDefinition("testContextProvider", RootBeanDefinition(TestContextProvider::class.java))
                refresh()
            }
        val service = BotActionLocatorService(applicationContext, allowFeatures)

        val action = service.findBotAction("telegram_bot", "test", "option", "joinFree")

        assertThat(action).isNotNull
        assertThat(requireNotNull(action).beanMethod.name).isEqualTo("handleOption")
    }

    @Test
    fun `does not locate feature commands when the bot registry denies the feature`() {
        val applicationContext =
            GenericApplicationContext().apply {
                registerBeanDefinition("testCommand", RootBeanDefinition(FeatureCommand::class.java))
                registerBeanDefinition("testContextProvider", RootBeanDefinition(TestContextProvider::class.java))
                refresh()
            }
        val service = BotActionLocatorService(applicationContext, denyFeatures)

        val initAction = service.findBotAction("max_bot", "game", "init", null)
        val callbackAction = service.findBotAction("max_bot", "game", "option", "joinFree")

        assertThat(initAction).isNull()
        assertThat(callbackAction).isNull()
    }

    @BotCommand(name = "test")
    private class TestCommand {
        @BotAction(onState = "winner", onCallback = BotAction.ANY_CALLBACK)
        fun handleWinner(context: BotActionContext): ActionResult = throw UnsupportedOperationException(context.toString())

        @BotAction(onState = "option", onCallback = BotAction.ANY_CALLBACK)
        fun handleOption(context: BotActionContext): ActionResult = throw UnsupportedOperationException(context.toString())
    }

    @BotCommand(name = "game", features = [BotFeature.GAMIFICATION])
    private class FeatureCommand {
        @BotAction
        fun execute(context: BotActionContext): ActionResult = throw UnsupportedOperationException(context.toString())

        @BotAction(onState = "option", onCallback = BotAction.ANY_CALLBACK)
        fun handleOption(context: BotActionContext): ActionResult = throw UnsupportedOperationException(context.toString())
    }

    private class TestContextProvider : BotActionContextProvider<BotActionContext> {
        override fun resolve(
            botUsername: String,
            payloadData: ChatUtils.PayloadData,
        ): BotActionContext = BotActionContext(botUsername, payloadData, null)
    }

    companion object {
        private val allowFeatures = BotFeatureSupport { _, _ -> true }
        private val denyFeatures = BotFeatureSupport { _, _ -> false }
    }
}
