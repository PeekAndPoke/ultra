package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/** Findings of the operator audit that need real threads and time (ULTRA-00309). */
class AuditJvmSpec : StringSpec({

    "asFlow: a slow collector gets the latest value, not the 64 first" {
        val source = StreamSource(0)
        val received = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()

        val job = launch(Dispatchers.Default) {
            source.asFlow().collect {
                if (it == 0) gate.await()
                synchronized(received) { received.add(it) }
            }
        }

        while (source.subscriptions.isEmpty()) delay(1)
        repeat(100) { source(it + 1) }
        gate.complete(Unit)
        delay(200)
        job.cancel()

        synchronized(received) { received.last() } shouldBe 100
    }

    "FrameClock.ticker with an interval of 0 does not run frames back to back" {
        val frames = AtomicInteger(0)
        val clock = FrameClock.ticker(intervalMs = 0)
        // A flag the frames read: a cancel handle written on pool threads could be stale
        val running = AtomicBoolean(true)

        fun next() {
            clock.next {
                if (running.get()) {
                    frames.incrementAndGet()
                    next()
                }
            }
        }

        next()
        delay(100)
        running.set(false)

        // At least 1 ms apart: about 100 frames; back to back: tens of thousands
        frames.get() shouldBeLessThan 500
    }

    "debounce publishes on the context it was given" {
        val thread = Executors.newSingleThreadExecutor { Thread(it, "debounce-test") }
        val source = StreamSource(0)
        val debounced = source.debounce(delay = 10.milliseconds, context = thread.asCoroutineDispatcher())
        val names = mutableListOf<String>()

        debounced.subscribeToStream { if (it == 1) synchronized(names) { names.add(Thread.currentThread().name.substringBefore(" @")) } }
        source(1)
        delay(200)
        thread.shutdown()

        synchronized(names) { names } shouldBe listOf("debounce-test")
    }
})
