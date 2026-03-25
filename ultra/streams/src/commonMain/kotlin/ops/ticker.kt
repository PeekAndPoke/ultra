package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeSource

/** A single frame emitted by a ticker stream */
data class TickerFrame(val count: Int, val currentTime: Double, val deltaTime: Double)

/** Creates a ticker stream that emits a [TickerFrame] at the given [intervalMs] */
fun ticker(intervalMs: Int): Stream<TickerFrame> = CoroutineTicker(intervalMs)

/** Creates a ticker stream that emits a [TickerFrame] at the given [interval] */
fun ticker(interval: Duration): Stream<TickerFrame> = ticker(interval.inWholeMilliseconds.toInt())

/**
 * Coroutine-based ticker implementation.
 *
 * Only runs while there are active subscribers. Emits [TickerFrame] with
 * monotonic timing for accurate deltaTime calculation.
 */
internal class CoroutineTicker(
    private val intervalMs: Int,
) : StreamSourceImpl<TickerFrame>(TickerFrame(count = 0, currentTime = 0.0, deltaTime = 0.0)) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private var count: Int = 0
    private var previousMark: TimeSource.Monotonic.ValueTimeMark? = null
    private val startMark = TimeSource.Monotonic.markNow()

    override fun onSub() {
        if (job == null) {
            job = scope.launch {
                while (isActive) {
                    delay(intervalMs.toLong())

                    val now = TimeSource.Monotonic.markNow()
                    val delta = previousMark?.let { (now - it).inWholeMilliseconds.toDouble() } ?: 0.0
                    val elapsed = (now - startMark).inWholeMilliseconds.toDouble()
                    previousMark = now
                    count++

                    this@CoroutineTicker.invoke(
                        TickerFrame(count = count, currentTime = elapsed, deltaTime = delta)
                    )
                }
            }
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            job?.cancel()
            job = null
            previousMark = null
        }
    }
}
