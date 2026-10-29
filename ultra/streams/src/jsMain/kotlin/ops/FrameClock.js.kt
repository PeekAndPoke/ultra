package io.peekandpoke.ultra.streams.ops

import kotlinx.browser.window

/**
 * The browser's animation frames: in step with the screen, timed like `performance.now()`. All callers share one
 * `requestAnimationFrame` per frame (ULTRA-00269): the callbacks run in the order they asked, with the frame's time.
 */
val FrameClock.Companion.animationFrames: FrameClock get() = AnimationFrameClock

/**
 * One browser frame request for everyone waiting. A [next] from inside a callback waits for the following frame, as a
 * nested `requestAnimationFrame` does; a callback that throws is reported as uncaught, as the browser does, and the
 * others still run.
 */
private object AnimationFrameClock : FrameClock {

    private class Waiting(val onFrame: (timeMs: Double) -> Unit) {
        var cancelled = false
    }

    private var waiting = mutableListOf<Waiting>()

    private var requested: Int? = null

    override fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit {
        val entry = Waiting(onFrame)

        waiting.add(entry)

        if (requested == null) {
            requested = window.requestAnimationFrame(::frame)
        }

        return {
            if (!entry.cancelled) {
                entry.cancelled = true
                waiting.remove(entry)

                if (waiting.isEmpty()) {
                    requested?.let { window.cancelAnimationFrame(it) }
                    requested = null
                }
            }
        }
    }

    private fun frame(timeMs: Double) {
        // Taken before running: what asks during the run waits for the next frame
        val batch = waiting
        waiting = mutableListOf()
        requested = null

        batch.forEach { entry ->
            if (!entry.cancelled) {
                // Spent: a cancel from now on is a no-op
                entry.cancelled = true

                try {
                    entry.onFrame(timeMs)
                } catch (e: dynamic) {
                    // dynamic: JS code may throw what is not an Error, and the batch must still go on
                    // Reported on a task of its own, as an uncaught error with its stack, after the others ran
                    window.setTimeout({ throw e }, 0)
                }
            }
        }
    }
}

internal actual val platformFrameClock: FrameClock get() = FrameClock.animationFrames
