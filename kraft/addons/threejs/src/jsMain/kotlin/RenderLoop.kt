package io.peekandpoke.kraft.addons.threejs

import io.peekandpoke.ultra.streams.ops.FrameClock

/**
 * The loop behind [ThreeJsComponent], on a [FrameClock] (ULTRA-00270): a frame calls [onFrame] with the time since the
 * last frame and the running time, then asks for the next. While paused no frame is asked for at all.
 *
 * Time across a pause (the maintainer, 2026-10-29): the first frame after [resume] has a delta of 0, and the elapsed
 * time counts running time only, so a scene goes on where it rested instead of jumping.
 */
internal class RenderLoop(
    private val clock: FrameClock,
    private val onFrame: (deltaMs: Double, elapsedMs: Double) -> Unit,
) {
    private var cancelFrame: (() -> Unit)? = null

    private var running = false

    private var paused = false

    /** The time of the last frame; null before the first frame after a start or a resume. */
    private var lastFrameAt: Double? = null

    /** Running time so far: the sum of the deltas. */
    private var elapsedMs = 0.0

    /** Starts the loop from elapsed 0; no frame while it is paused. */
    fun start() {
        stop()
        running = true
        elapsedMs = 0.0
        requestFrame()
    }

    /** Asks for no more frames until [resume]. */
    fun pause() {
        paused = true
        cancel()
    }

    /** Goes on after a [pause], with a delta of 0 on the first frame. */
    fun resume() {
        if (!paused) return

        paused = false
        lastFrameAt = null
        requestFrame()
    }

    /** Stops the loop; a pending frame is cancelled. */
    fun stop() {
        running = false
        cancel()
    }

    private fun cancel() {
        cancelFrame?.invoke()
        cancelFrame = null
        lastFrameAt = null
    }

    private fun requestFrame() {
        if (running && !paused && cancelFrame == null) {
            cancelFrame = clock.next(::frame)
        }
    }

    private fun frame(at: Double) {
        // This frame's handle stays set while onFrame runs: a pause, a stop or a resume meanwhile decides what is next
        val handle = cancelFrame

        val delta = lastFrameAt?.let { at - it }?.coerceAtLeast(0.0) ?: 0.0
        lastFrameAt = at
        elapsedMs += delta

        onFrame(delta, elapsedMs)

        if (cancelFrame === handle) {
            cancelFrame = null
            requestFrame()
        }
    }
}
