package io.github.pm665.telegramboot.max

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaxMessageRateLimiterTest {
    @Test
    fun `spaces requests in one dialog by half a second but leaves other dialogs independent`() {
        var now = 0L
        val sleeps = mutableListOf<Long>()
        val limiter =
            MaxMessageRateLimiter(
                nowNanos = { now },
                sleepNanos = { duration ->
                    sleeps += duration
                    now += duration
                },
            )

        limiter.waitForTurn("max-a", 701)
        limiter.waitForTurn("max-a", 701)
        limiter.waitForTurn("max-a", 702)
        limiter.waitForTurn("max-b", 701)

        assertThat(sleeps).containsExactly(500_000_000L)
    }
}
