package io.peekandpoke.ultra.vault.hooks

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable

/**
 * Stamps [TimestampedMillis] entities with the current epoch millis on their way into the database.
 *
 * On insert both `createdMs` and `updatedMs` become 'now'. On update only `updatedMs` moves — as
 * long as the saved value still carries the stored `createdMs`, see
 * [TimestampedMillis.withTimestamps]. 'Now' comes from the injected [Kronos], so a test can pin it
 * with `Kronos.fixed(...)`.
 *
 * Nothing applies this globally: hand [onBeforeSave] to the `Repository.Hooks` of every repository
 * that should be stamped.
 */
class TimestampedMillisHook(
    private val kronos: Lazy<Kronos>,
) {
    private class OnBeforeSave<T : TimestampedMillis>(kronos: Lazy<Kronos>) : Repository.Hooks.OnBeforeSave<T> {

        private val kronos: Kronos by kronos

        override fun <X : T> onBeforeSave(repo: Repository<T>, storable: Storable<T>): Storable<X> {
            // X is chosen by the caller and erased at runtime, so this only holds as long as
            // withTimestamps() returns the receiver's own type.
            @Suppress("UNCHECKED_CAST")
            return storable.withValue(
                storable.valueInternal.withTimestamps(
                    kronos.millisNow()
                ) as X
            )
        }
    }

    /** Creates a before-save hook for a repository storing entities of type [T]. */
    fun <T : TimestampedMillis> onBeforeSave(): Repository.Hooks.OnBeforeSave<T> = OnBeforeSave(kronos)
}
