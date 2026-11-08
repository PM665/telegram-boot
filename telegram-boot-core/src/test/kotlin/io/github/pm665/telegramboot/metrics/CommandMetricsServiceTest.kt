package io.github.pm665.telegramboot.metrics

import io.github.pm665.telegramboot.infrastructure.metrics.CommandMetricsService
import io.github.pm665.telegramboot.infrastructure.metrics.CommandResultStatus
import io.micrometer.core.instrument.search.MeterNotFoundException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommandMetricsServiceTest {
    private lateinit var registry: SimpleMeterRegistry
    private lateinit var service: CommandMetricsService

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        service = CommandMetricsService(registry)
    }

    @Test
    fun `in flight gauge reflects current state`() {
        service.commandInFlight(BOT, CHAT_ID, COMMAND, "waitingForPhone", "ok")

        assertThat("waitingForPhone".gaugeValue()).isEqualTo(1.0)

        service.commandInFlight(BOT, CHAT_ID, COMMAND, "waitingForPhone", "invalidInput")

        assertThat("waitingForPhone".gaugeValue()).isEqualTo(1.0)
    }

    @Test
    fun `completing command removes gauge and increments counter`() {
        service.commandInFlight(BOT, CHAT_ID, COMMAND, "waitingForPhone", "ok")

        service.commandCompleted(BOT, CHAT_ID, COMMAND, "ok")

        assertThatThrownBy { "waitingForPhone".gaugeValue() }.isInstanceOf(MeterNotFoundException::class.java)
        assertThat(counterValue(CommandResultStatus.COMPLETED.value)).isEqualTo(1.0)
    }

    @Test
    fun `forgotten command is reported with dedicated status`() {
        service.commandInFlight(BOT, CHAT_ID, COMMAND, "waitingForPhone", "ok")

        service.commandForgotten(BOT, CHAT_ID, COMMAND)

        assertThatThrownBy { "waitingForPhone".gaugeValue() }.isInstanceOf(MeterNotFoundException::class.java)
        assertThat(counterValue(CommandResultStatus.FORGOTTEN.value)).isEqualTo(1.0)
    }

    private fun String.gaugeValue(): Double =
        registry
            .get("bot_commands_in_flight")
            .tag("bot", BOT)
            .tag("command", COMMAND)
            .tag("status", this)
            .gauge()
            .value()

    private fun counterValue(result: String): Double =
        registry
            .get("bot_commands_completed")
            .tag("bot", BOT)
            .tag("command", COMMAND)
            .tag("result", result)
            .counter()
            .count()

    companion object {
        private const val BOT = "bot"
        private const val COMMAND = "register"
        private const val CHAT_ID = 42L
    }
}
