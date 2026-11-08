package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.micrometer.context.ContextExecutorService
import io.micrometer.context.ContextSnapshotFactory
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.Executors

internal class TelegramBotUpdateConsumer(
    private val botUsername: String,
    private val mapper: TelegramUpdateMapper,
    private val handler: BotUpdateHandler,
) : LongPollingUpdateConsumer {
    private val executor =
        ContextExecutorService.wrap(
            Executors.newVirtualThreadPerTaskExecutor(),
            ContextSnapshotFactory.builder().build(),
        )

    override fun consume(updates: List<Update>) {
        updates.forEach { update ->
            executor.execute { handler.handleUpdate(botUsername, mapper.from(update)) }
        }
    }
}
