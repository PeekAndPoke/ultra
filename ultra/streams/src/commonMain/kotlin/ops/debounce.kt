package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay as suspendFor
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Debounce the incoming values by the given [delayMs].
 *
 * Only the last value received within the delay window is published. See the overload with a `Stream<Duration>`.
 */
fun <T> Stream<T>.debounce(delayMs: Int): Stream<T> = debounce(delay = steady(delayMs.milliseconds))

/** Debounce the incoming values by the given [delay]; see the overload with a `Stream<Duration>`. */
fun <T> Stream<T>.debounce(delay: Duration, context: CoroutineContext = Dispatchers.Default): Stream<T> =
    debounce(delay = steady(delay), context = context)

/**
 * Debounce the incoming values by the [delay] it holds now, which may change (ULTRA-00271): a new delay applies to the
 * next value, a pending one keeps its own. Only the last value received within the delay window is published; a delay
 * of zero or less publishes at once.
 *
 * A new subscriber is told the value as it is; nothing is published again for it. Read while someone listens, the
 * stream gives the last value it passed on; while nobody listens, the source's (ULTRA-00302). The source and [delay]
 * are listened to only while someone listens here. Values are published on [context]: on the JVM and native pass the
 * context the subscribers run on (default: a pool thread).
 */
fun <T> Stream<T>.debounce(delay: Stream<Duration>, context: CoroutineContext = Dispatchers.Default): Stream<T> {
    val scope = CoroutineScope(context + SupervisorJob())

    return debounce(delay = delay) { wait, publish ->
        val job = scope.launch {
            suspendFor(wait)
            publish()
        }

        return@debounce { job.cancel() }
    }
}

/** The debounce operator, with the timer injectable for tests: [after] runs a block after a delay, and cancels it. */
internal fun <T> Stream<T>.debounce(
    delay: Stream<Duration>,
    after: (wait: Duration, block: () -> Unit) -> () -> Unit,
): Stream<T> = StreamDebounceOperator(source = this, delay = delay, after = after)

private class StreamDebounceOperator<T>(
    private val source: Stream<T>,
    private val delay: Stream<Duration>,
    private val after: (wait: Duration, block: () -> Unit) -> () -> Unit,
) : StreamSourceImpl<T>(source()) {

    private var listening = false

    /** Set while [onSub] subscribes: the source's value is taken as it is, no timer for it. */
    private var starting = false

    private var unsubscribeSource: Unsubscribe? = null

    private var unsubscribeDelay: Unsubscribe? = null

    /** The delay for the next value: [delay]'s latest while someone listens. */
    private var wait: Duration = Duration.ZERO

    /** The pending value's cancel. */
    private var cancelPending: (() -> Unit)? = null

    /** The pending value's own token: a timer that fires after its cancel (another thread) publishes nothing. */
    private var pending: Any? = null

    override fun invoke(): T = if (listening) current else source()

    override fun onSub() {
        if (listening) return

        listening = true
        starting = true

        try {
            unsubscribeDelay = delay.subscribeToStream { wait = it }
            unsubscribeSource = source.subscribeToStream { incoming(it) }
        } catch (e: Throwable) {
            // Half subscribed: undone, so the next subscriber starts afresh
            stopListening()
            throw e
        } finally {
            starting = false
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            stopListening()
        }
    }

    private fun stopListening() {
        listening = false
        cancelPending?.invoke()
        cancelPending = null
        pending = null
        unsubscribeSource?.invoke()
        unsubscribeSource = null
        unsubscribeDelay?.invoke()
        unsubscribeDelay = null
    }

    private fun incoming(value: T) {
        cancelPending?.invoke()
        cancelPending = null
        pending = null

        when {
            starting -> current = value

            wait <= Duration.ZERO -> invoke(value)

            else -> {
                val token = Any()
                pending = token

                // The block only checks its token: clearing the fields here could race a newer value on another thread
                cancelPending = after(wait) {
                    if (pending === token) invoke(value)
                }
            }
        }
    }
}
