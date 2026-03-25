package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Converts the [Stream] into a [Flow].
 *
 * The Flow immediately emits the stream's current value, then emits
 * subsequent values as they are published. Unsubscribes when the Flow is cancelled.
 */
fun <T> Stream<T>.asFlow(): Flow<T> = callbackFlow {
    val unsub = subscribeToStream { trySend(it) }
    awaitClose { unsub() }
}
