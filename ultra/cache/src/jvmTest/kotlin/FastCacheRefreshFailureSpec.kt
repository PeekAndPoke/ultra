package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * How a failing refresh loader is handled.
 *
 * JVM only: an `Error` escaping a coroutine reaches the platform's last-resort handler, which on
 * native ends the process.
 */
class FastCacheRefreshFailureSpec : StringSpec({

    /** Runs [block] with stdout captured; returns what was printed. */
    suspend fun capturingStdout(block: suspend () -> Unit): String {
        val original = System.out
        val buffer = ByteArrayOutputStream()

        System.setOut(PrintStream(buffer, true))

        try {
            block()
        } finally {
            System.setOut(original)
        }

        return buffer.toString()
    }

    "a failing refresh is reported without its key or the exception's message" {
        // a key or a message can carry a session id or a token
        var loads = 0

        // a scope of its own, cancelled at the end, so nothing prints into the next test
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val output = capturingStdout {
            val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
                refreshAfterWrite(50.milliseconds) { loads++; throw IllegalStateException("secret-message") }
            }

            cache.put("secret-key", "1")

            eventually(3.seconds) { (loads >= 1) shouldBe true }
            Thread.sleep(50)
        }

        scope.cancel()

        output shouldContain "IllegalStateException"
        output shouldNotContain "secret"
    }

    "a failing behaviour is reported without the exception's message" {
        var calls = 0
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val output = capturingStdout {
            fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
                addBehaviour(object : FastCache.Behaviour<String, String> {
                    override fun process(cache: FastCache<String, String>, updates: FastCache.ActionUpdates<String, String>) {
                        calls++
                        throw IllegalStateException("secret-message")
                    }
                })
            }

            eventually(3.seconds) { (calls >= 1) shouldBe true }
            Thread.sleep(50)
        }

        scope.cancel()

        output shouldContain "IllegalStateException"
        output shouldNotContain "secret"
    }

    "a loader throwing an Error releases its key for the next refresh" {
        val loads = AtomicInteger(0)

        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            refreshAfterWrite(50.milliseconds) {
                if (loads.incrementAndGet() == 1) throw AssertionError("first load fails with an Error")
                it
            }
        }

        cache.put("a", "1")

        eventually(3.seconds) {
            loads.get() shouldBeGreaterThanOrEqual 2
        }
    }
})
