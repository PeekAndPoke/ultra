package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSourceImpl
import kotlinx.browser.window
import kotlin.js.Date
import kotlin.math.round
import kotlin.time.Duration

fun ticker(intervalMs: Int): Stream<Long> = Ticker(intervalMs)

fun ticker(interval: Duration): Stream<Long> = ticker(interval.inWholeMilliseconds.toInt())

internal class Ticker(
    private val intervalMs: Int,
) : StreamSourceImpl<Long>(0) {

    private val start = Date.now()

    private var interval: Int? = null

    override fun onSub() {
        if (interval == null) {
            interval = window.setInterval(::handler, intervalMs)
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty() && interval != null) {
            window.clearInterval(interval!!)
            interval = null
        }
    }

    private fun handler() {
        val now = Date.now()

        val counter = round((now - start) / intervalMs).toLong()

        this.invoke(counter)
    }
}
