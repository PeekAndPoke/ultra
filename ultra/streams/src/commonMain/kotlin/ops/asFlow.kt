package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Converts the [Stream] into a [Flow].
 *
 * The Flow immediately emits the stream's current value, then emits
 * subsequent values as they are published. Unsubscribes when the Flow is cancelled.
 *
 * A collector slower than the stream gets the latest value, not every value (ULTRA-00309): it is conflated.
 */
fun <T> Stream<T>.asFlow(): Flow<T> = callbackFlow {
    val unsub = subscribeToStream { trySend(it) }
    awaitClose { unsub() }
}.conflate()
