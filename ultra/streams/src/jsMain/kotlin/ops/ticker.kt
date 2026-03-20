package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSourceImpl
import kotlinx.browser.window
import kotlin.js.Date
import kotlin.time.Duration

data class TickerFrame(val count: Int, val currentTime: Double, val deltaTime: Double)

private fun initial() = TickerFrame(count = 0, currentTime = Date.now(), deltaTime = 0.0)

fun ticker(intervalMs: Int): Stream<TickerFrame> = Ticker(intervalMs)

fun ticker(interval: Duration): Stream<TickerFrame> = ticker(interval.inWholeMilliseconds.toInt())

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

internal class Ticker(
    private val intervalMs: Int,
) : StreamSourceImpl<TickerFrame>(initial()) {

    private var previousTime: Double? = null
    private var interval: Int? = null
    private var count: Int = 0

    override fun onSub() {
        if (interval == null) {
            interval = window.setInterval(::handler, intervalMs)
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty() && interval != null) {
            window.clearInterval(interval!!)
            interval = null
            previousTime = null
        }
    }

    private fun handler() {
        val now = Date.now()
        val delta = previousTime?.let { now - it } ?: 0.0
        previousTime = now
        count++

        this.invoke(TickerFrame(count = count, currentTime = now, deltaTime = delta))
    }
}
