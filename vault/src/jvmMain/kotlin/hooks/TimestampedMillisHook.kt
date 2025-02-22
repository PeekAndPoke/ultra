package de.peekandpoke.ultra.vault.hooks

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable

class TimestampedMillisHook(
    private val kronos: Lazy<Kronos>,
) {
    private class OnBeforeSave<T : TimestampedMillis>(kronos: Lazy<Kronos>) : Repository.Hooks.OnBeforeSave<T> {

        private val kronos: Kronos by kronos

        override fun <X : T> onBeforeSave(repo: Repository<T>, storable: Storable<T>): Storable<X> {
            @Suppress("UNCHECKED_CAST")
            return storable.withValue(
                storable.value.withTimestamps(
                    kronos.millisNow()
                ) as X
            )
        }
    }

    fun <T : TimestampedMillis> onBeforeSave(): Repository.Hooks.OnBeforeSave<T> = OnBeforeSave(kronos)
}
