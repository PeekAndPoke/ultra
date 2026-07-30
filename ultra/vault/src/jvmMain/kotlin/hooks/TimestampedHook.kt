package io.peekandpoke.ultra.vault.hooks

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable

/**
 * Stamps [Timestamped] entities with the current time on their way into the database.
 *
 * On insert both `createdAt` and `updatedAt` become 'now'. On update only `updatedAt` moves — as
 * long as the saved value still carries the stored `createdAt`, see [Timestamped.withTimestamps].
 * 'Now' comes from the injected [Kronos], so a test can pin it with `Kronos.fixed(...)`.
 *
 * Nothing applies this globally: hand [onBeforeSave] to the `Repository.Hooks` of every repository
 * that should be stamped.
 */
class TimestampedHook(
    private val kronos: Lazy<Kronos>,
) {
    private class OnBeforeSave<T : Timestamped>(kronos: Lazy<Kronos>) : Repository.Hooks.OnBeforeSave<T> {

        private val kronos: Kronos by kronos

        override fun <X : T> onBeforeSave(repo: Repository<T>, storable: Storable<T>): Storable<X> {
            // X is chosen by the caller and erased at runtime, so this only holds as long as
            // withTimestamps() returns the receiver's own type.
            @Suppress("UNCHECKED_CAST")
            return storable.withValue(
                storable.valueInternal.withTimestamps(
                    kronos.instantNow()
                ) as X
            )
        }
    }

    /** Creates a before-save hook for a repository storing entities of type [T]. */
    fun <T : Timestamped> onBeforeSave(): Repository.Hooks.OnBeforeSave<T> = OnBeforeSave(kronos)
}
