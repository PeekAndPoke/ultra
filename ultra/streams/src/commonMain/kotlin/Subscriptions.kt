package de.peekandpoke.ultra.streams

/**
 * Collects stream subscriptions for batch cleanup.
 *
 * Useful for components, scopes, or any context where multiple streams are subscribed
 * and should all be unsubscribed together (e.g., when a UI component unmounts).
 *
 * Example:
 * ```
 * val subs = Subscriptions()
 *
 * subs.subscribe(nameStream) { println(it) }
 * subs.subscribe(ageStream) { println(it) }
 *
 * // Later — cleans up all subscriptions at once
 * subs.unsubscribeAll()
 * ```
 */
class Subscriptions {

    private val entries = mutableListOf<Unsubscribe>()

    /** The number of active subscriptions */
    val size: Int get() = entries.size

    /** Adds an existing [unsubscribe] token to be managed */
    fun add(unsubscribe: Unsubscribe) {
        entries.add(unsubscribe)
    }

    /** Subscribes to a [stream] and manages the resulting subscription */
    fun <T> subscribe(stream: Stream<T>, handler: StreamHandler<T>): Unsubscribe {
        val unsub = stream.subscribeToStream(handler)
        entries.add(unsub)
        return unsub
    }

    /** Unsubscribes all managed subscriptions and clears the list */
    fun unsubscribeAll() {
        entries.forEach { it() }
        entries.clear()
    }

    /** Operator shorthand: `subs += stream.subscribeToStream { ... }` */
    operator fun plusAssign(unsubscribe: Unsubscribe) {
        add(unsubscribe)
    }
}
