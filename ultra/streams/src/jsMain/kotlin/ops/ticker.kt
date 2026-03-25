package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import kotlinx.browser.window
import kotlin.js.Date

private fun initial() = TickerFrame(count = 0, currentTime = Date.now(), deltaTime = 0.0)

/** Creates an animation-frame ticker stream using requestAnimationFrame. JS-only. */
fun animTicker(): Stream<TickerFrame> = AnimTicker()

internal class AnimTicker : StreamSourceImpl<TickerFrame>(initial()) {

    private var requestId: Int? = null
    private var previousTime: Double? = null
    private var count: Int = 0

    override fun onSub() {
        if (requestId == null) {
            scheduleFrame()
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty() && requestId != null) {
            window.cancelAnimationFrame(requestId!!)
            requestId = null
            previousTime = null
        }
    }

    private fun scheduleFrame() {
        requestId = window.requestAnimationFrame(::handler)
    }

    private fun handler(timestamp: Double) {
        val delta = previousTime?.let { timestamp - it } ?: 0.0
        previousTime = timestamp
        count++

        this.invoke(TickerFrame(count = count, currentTime = timestamp, deltaTime = delta))

        if (subscriptions.isNotEmpty()) {
            scheduleFrame()
        } else {
            requestId = null
        }
    }
}
