package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Counts the clock's calls of `window.requestAnimationFrame` and `cancelAnimationFrame` while [block] runs. */
private suspend fun countingFrames(block: suspend (Counts) -> Unit) {
    val counts = Counts()
    val win = window.asDynamic()
    val request = win.requestAnimationFrame
    val cancel = win.cancelAnimationFrame

    win.requestAnimationFrame = { callback: dynamic ->
        counts.requested++
        request.call(window, callback)
    }
    win.cancelAnimationFrame = { id: dynamic ->
        counts.cancelled++
        cancel.call(window, id)
    }

    try {
        block(counts)
    } finally {
        win.requestAnimationFrame = request
        win.cancelAnimationFrame = cancel
    }
}

private class Counts {
    var requested = 0
    var cancelled = 0
}

/** Waits two browser frames, past the one the clock asked for. */
private suspend fun afterFrames() {
    repeat(2) {
        suspendCancellableCoroutine { cont -> window.requestAnimationFrame { cont.resume(Unit) } }
    }
}

class AnimationFrameClockSpec : StringSpec({

    val clock = FrameClock.animationFrames

    "nine callers in one frame share one requestAnimationFrame, in order, with the same time" {
        countingFrames { counts ->
            val calls = mutableListOf<Pair<Int, Double>>()

            repeat(9) { n -> clock.next { at -> calls.add(n to at) } }

            counts.requested shouldBe 1

            afterFrames()

            calls.map { it.first } shouldBe (0 until 9).toList()
            calls.map { it.second }.distinct().size shouldBe 1
        }
    }

    "a cancelled caller is skipped, the others still run" {
        val calls = mutableListOf<String>()

        clock.next { calls.add("a") }
        val cancelB = clock.next { calls.add("b") }
        clock.next { calls.add("c") }

        cancelB()
        afterFrames()

        calls shouldBe listOf("a", "c")
    }

    "cancelling every caller cancels the frame request" {
        countingFrames { counts ->
            var ran = false

            val first = clock.next { ran = true }
            val second = clock.next { ran = true }

            first()
            counts.cancelled shouldBe 0
            second()
            counts.cancelled shouldBe 1

            afterFrames()
            ran shouldBe false
        }
    }

    "a caller asking from inside a frame waits for the following frame" {
        val times = mutableListOf<Double>()

        clock.next { first ->
            times.add(first)
            clock.next { second -> times.add(second) }
        }

        afterFrames()
        afterFrames()

        times.size shouldBe 2
        (times[1] > times[0]) shouldBe true
    }

    "a caller that throws does not stop the others, and its error is reported as uncaught" {
        val calls = mutableListOf<String>()
        var reported: Any? = null
        val handler = window.onerror

        window.onerror = { _, _, _, _, error ->
            reported = error
            true
        }

        try {
            clock.next { calls.add("before") }
            clock.next { throw IllegalStateException("boom") }
            clock.next { calls.add("after") }

            afterFrames()
            // The error is rethrown on a task of its own
            delay(20)
        } finally {
            window.onerror = handler
        }

        calls shouldBe listOf("before", "after")
        (reported as? Throwable)?.message shouldBe "boom"
    }
})
