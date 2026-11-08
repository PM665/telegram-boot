package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.features.bots.InMemoryBotsService
import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageTextProcessor
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier

class MaxAutoConfigurationTest {
    @Test
    fun `registers MAX sender lifecycle and webhook through auto configuration`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaxConfiguration::class.java))
            .withBean(ObjectMapper::class.java, Supplier { ObjectMapper() })
            .withBean(BotsService::class.java, Supplier { InMemoryBotsService() })
            .withBean(UsersService::class.java, Supplier { mock<UsersService>() })
            .withBean(MessageTextProcessor::class.java, Supplier { mock<MessageTextProcessor>() })
            .withBean(BotUpdateHandler::class.java, Supplier { mock<BotUpdateHandler>() })
            .withBean(TaskExecutorService::class.java, Supplier { mock<TaskExecutorService>() })
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(MaxMessageSender::class.java)
                assertThat(context).hasSingleBean(MaxBotManager::class.java)
                assertThat(context).hasSingleBean(MaxWebhookController::class.java)
                assertThat(context).hasSingleBean(MaxPlatformBotTokenValidator::class.java)
            }
    }
}
