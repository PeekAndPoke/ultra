package io.peekandpoke.ultra.streams.ops

import kotlinx.browser.window

/** The browser's animation frames: in step with the screen, timed like `performance.now()`. */
val FrameClock.Companion.animationFrames: FrameClock get() = AnimationFrameClock

private val AnimationFrameClock = FrameClock { onFrame ->
    val id = window.requestAnimationFrame { onFrame(it) }

    ({ window.cancelAnimationFrame(id) })
}

internal actual val platformFrameClock: FrameClock get() = FrameClock.animationFrames
