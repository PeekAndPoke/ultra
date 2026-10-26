package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import kotlinx.browser.window

/** Creates an animation-frame ticker stream using requestAnimationFrame. JS-only. */
fun animTicker(): Stream<TickerFrame> = ticker(clock = FrameClock.animationFrames, startTime = window.performance.now())
