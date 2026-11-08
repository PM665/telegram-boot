package io.github.pm665.telegramboot.max

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal class MaxMessageRateLimiter(
    private val intervalNanos: Long = TimeUnit.MILLISECONDS.toNanos(500),
    private val nowNanos: () -> Long = System::nanoTime,
    private val sleepNanos: (Long) -> Unit = { TimeUnit.NANOSECONDS.sleep(it) },
) {
    private class DialogState(var lastRequestNanos: Long? = null)

    private val dialogs = ConcurrentHashMap<Pair<String, Long>, DialogState>()

    fun waitForTurn(
        botUsername: String,
        userId: Long,
    ) {
        val state = dialogs.computeIfAbsent(botUsername to userId) { DialogState() }
        synchronized(state) {
            val now = nowNanos()
            val nextAvailable = state.lastRequestNanos?.plus(intervalNanos)
            if (nextAvailable != null && now < nextAvailable) sleepNanos(nextAvailable - now)
            state.lastRequestNanos = nowNanos()
        }
    }
}
