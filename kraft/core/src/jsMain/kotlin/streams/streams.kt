package de.peekandpoke.kraft.streams

import de.peekandpoke.kraft.streams.addons.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion

/**
 * The [Unsubscribe] function is returned when a subscription is created on a [Stream].
 *
 * Calling this function cancels the subscription.
 */
typealias Unsubscribe = () -> Unit

/**
 * Handler-function for the incoming values of a [Stream].
 */
typealias StreamHandler<T> = (T) -> Unit

/**
 * Subscribes to the stream permanently
 *
 * NOTICE: there is no way to get rid of the subscription anymore. Use with caution.
 */
fun <T> Stream<T>.permanent(): Stream<T> = permanent { }

/**
 * Subscribes to the stream permanently
 *
 * NOTICE: there is no way to get rid of the subscription anymore. Use with caution.
 */
fun <T> Stream<T>.permanent(handler: (T) -> Unit): Stream<T> = apply { subscribeToStream(handler) }

/**
 * Converts the [Stream] into a [Flow]
 */
fun <T> Stream<T>.asFlow(): Flow<T> {
    val stream = this
    var sub: Unsubscribe? = null
    var count = 0

    return channelFlow {
        channel.send(stream())

        sub = stream.mapAsync {
            if (count++ > 0) {
                channel.send(it)
            }
        }.subscribeToStream()

    }.onCompletion {
        sub?.invoke()
    }
}
