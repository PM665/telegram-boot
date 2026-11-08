package io.github.pm665.telegramboot.infrastructure.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CommandMetricsService(
    private val meterRegistry: MeterRegistry,
) {
    private val inFlightGauge: MultiGauge = MultiGauge.builder(IN_FLIGHT_METRIC).register(meterRegistry)
    private val inFlightCommands = ConcurrentHashMap<InFlightCommandKey, InFlightCommand>()

    fun commandInFlight(
        botUsername: String,
        chatId: Long,
        commandName: String,
        state: String,
        lastResult: String,
    ) {
        val key = InFlightCommandKey(botUsername, chatId)
        val command = InFlightCommand(botUsername, chatId, commandName, sanitize(state), sanitize(lastResult))
        inFlightCommands[key] = command
        refreshInFlightGauge()
    }

    fun commandCompleted(
        botUsername: String,
        chatId: Long,
        commandName: String,
        resultStatus: String,
    ) {
        val key = InFlightCommandKey(botUsername, chatId)
        val removed = inFlightCommands.remove(key)
        refreshInFlightGauge()
        incrementCompletedCounter(botUsername, getCommandNameOrDefault(commandName, removed), resultStatus)
    }

    fun commandForgotten(
        botUsername: String,
        chatId: Long,
        commandName: String,
    ) {
        val key = InFlightCommandKey(botUsername, chatId)
        val removed = inFlightCommands.remove(key)
        refreshInFlightGauge()
        val finalCommandName = getCommandNameOrDefault(commandName, removed)
        if (!finalCommandName.isNullOrBlank()) {
            incrementCompletedCounter(botUsername, finalCommandName, CommandResultStatus.FORGOTTEN.value)
        }
    }

    private fun incrementCompletedCounter(
        botUsername: String,
        commandName: String?,
        resultStatus: String,
    ) {
        if (commandName.isNullOrBlank()) {
            return
        }
        val finalResult = CommandResultStatus.fromActionStatus(resultStatus)
        if (CommandResultStatus.FORGOTTEN.value == resultStatus) {
            recordFinalResult(botUsername, commandName, CommandResultStatus.FORGOTTEN)
        } else {
            recordFinalResult(botUsername, commandName, finalResult)
        }
    }

    private fun recordFinalResult(
        botUsername: String,
        commandName: String,
        finalResult: CommandResultStatus,
    ) {
        meterRegistry
            .counter(
                COMPLETED_METRIC,
                Tags.of(
                    Tag.of(TAG_BOT, botUsername),
                    Tag.of(TAG_COMMAND, commandName),
                    Tag.of(TAG_RESULT, finalResult.value),
                ),
            ).increment()
    }

    private fun getCommandNameOrDefault(
        commandName: String,
        removed: InFlightCommand?,
    ): String? =
        if (!commandName.isNullOrBlank()) {
            commandName
        } else {
            removed?.commandName
        }

    private fun refreshInFlightGauge() {
        val rows = mutableListOf<MultiGauge.Row<*>>()
        val aggregated = mutableMapOf<GaugeKey, Long>()

        for (value in inFlightCommands.values) {
            val key = GaugeKey(value.commandName, value.botUsername, value.state)
            aggregated.merge(key, 1L) { old, new -> old + new }
        }

        for ((key, count) in aggregated) {
            rows.add(
                MultiGauge.Row.of(
                    Tags.of(
                        Tag.of(TAG_COMMAND, key.command),
                        Tag.of(TAG_BOT, key.bot),
                        Tag.of(TAG_STATUS, key.state),
                    ),
                    count,
                ),
            )
        }
        inFlightGauge.register(rows, true)
    }

    private fun sanitize(value: String): String = if (value.isNotBlank()) value else UNKNOWN_STATUS

    private data class InFlightCommandKey(
        val botUsername: String,
        val chatId: Long,
    )

    private data class InFlightCommand(
        val botUsername: String,
        val chatId: Long,
        val commandName: String,
        val state: String,
        val lastResult: String,
    )

    private data class GaugeKey(
        val command: String,
        val bot: String,
        val state: String,
    )

    companion object {
        const val UNKNOWN_STATUS = "unknown"
        private const val IN_FLIGHT_METRIC = "bot_commands_in_flight"
        private const val COMPLETED_METRIC = "bot_commands_completed"
        private const val TAG_BOT = "bot"
        private const val TAG_COMMAND = "command"
        private const val TAG_STATUS = "status"
        private const val TAG_RESULT = "result"
    }
}
