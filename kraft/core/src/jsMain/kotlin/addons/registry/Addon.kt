package io.peekandpoke.kraft.addons.registry

import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource

/**
 * A lazily-loadable addon that implements [Stream]<T?>.
 *
 * Emits `null` while the addon is loading, and the loaded value once available.
 * Components access the addon via `subscribingTo(addons.myAddon)`.
 *
 * @param T the type of the loaded addon facade
 */
class Addon<T> internal constructor(
    /** Human-readable name for error messages. */
    val name: String,
    /** Whether to defer loading until the first subscription. */
    private val lazy: Boolean,
    /** Suspend function that performs the dynamic import and returns the addon facade. */
    private val loader: suspend () -> T,
) : Stream<T?> {

    private val source = StreamSource<T?>(null)
    private var loadStarted = false

    /** Current value, or null if not yet loaded. */
    override fun invoke(): T? {
        if (lazy) ensureLoading()
        return source()
    }

    /** Subscribe to value changes. For lazy addons, this triggers the load. */
    override fun subscribeToStream(sub: (T?) -> Unit): () -> Unit {
        if (lazy) ensureLoading()
        return source.subscribeToStream(sub)
    }

    /** Triggers the load if not already started. Called by the registry for non-lazy addons. */
    internal fun load() {
        ensureLoading()
    }

    private fun ensureLoading() {
        if (loadStarted) return
        loadStarted = true

        launch {
            try {
                val value = loader()
                source(value)
            } catch (e: Throwable) {
                console.error("Failed to load addon '$name': ${e.message}")
            }
        }
    }
}
