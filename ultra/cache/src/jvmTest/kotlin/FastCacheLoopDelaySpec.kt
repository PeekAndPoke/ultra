package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A loop delay of zero or less falls back to 1 ms instead of a loop that never suspends.
 *
 * JVM only: on JS a loop that never suspends would block the test's own thread.
 */
class FastCacheLoopDelaySpec : StringSpec({

    "a zero or negative loop delay runs as 1 ms, not as a busy loop" {
        listOf(Duration.ZERO, (-5).milliseconds).forEach { loopDelay ->
            val passes = AtomicInteger(0)
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val cache = fastCache<String, String>(scope = scope, loopDelay = loopDelay) {
                addBehaviour(object : FastCache.Behaviour<String, String> {
                    override fun process(
                        cache: FastCache<String, String>,
                        updates: FastCache.ActionUpdates<String, String>,
                    ) {
                        passes.incrementAndGet()
                    }
                })
            }

            Thread.sleep(300)

            // at 1 ms per pass at most about 300; a loop that never suspends makes millions
            passes.get() shouldBeLessThan 1_000
            passes.get() shouldBeGreaterThan 10

            scope.cancel()
            keepReachable(cache)
        }
    }
})
