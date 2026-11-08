package io.github.pm665.telegramboot.infrastructure

import io.micrometer.context.ContextSnapshotFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

@Service
class TaskExecutorService(
    private val taskScheduler: TaskScheduler,
    @Qualifier("telegramBootTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
) {
    private val contextSnapshotFactory = ContextSnapshotFactory.builder().build()

    fun executeImmediately(runnable: Runnable) {
        taskExecutor.execute(runnable)
    }

    fun executeDelayed(
        runnable: Runnable,
        delay: Duration,
    ) {
        taskScheduler.schedule(
            contextSnapshotFactory.captureAll().wrap(runnable),
            ZonedDateTime.now().plus(delay).toInstant(),
        )
    }
}
