package de.peekandpoke.ultra.streams

import de.peekandpoke.ultra.streams.ops.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion

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
