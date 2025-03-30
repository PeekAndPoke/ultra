package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import kotlin.time.Duration

class LockedStored<out T>(
    stored: Stored<T>,
) {
    private val original: Stored<T> = stored
    private var _stored: Stored<T> = stored

    val stored: Stored<T> get() = _stored

    val value: T get() = stored.value
    val id: String get() = stored._id
    val key: String get() = stored._key
    val rev: String get() = stored._rev

    val isModified get() = original != _stored

    fun modify(block: (T) -> @UnsafeVariance T): LockedStored<T> {
        _stored = _stored.modify(block)

        return this
    }

    inline fun <reified X : @UnsafeVariance T> castTyped(): LockedStored<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as LockedStored<X>
            else -> null
        }
    }
}

data class LockedStoredResult<T, out R>(
    val value: Stored<T>,
    val result: R?,
    val success: Boolean,
)

suspend fun <T : Any, X : T, R> GlobalLocksProvider.tryToLockReloadAndSave(
    stored: Stored<X>,
    repo: Repository<T>,
    timeout: Duration = Duration.ZERO,
    handler: suspend (LockedStored<X>) -> R,
): LockedStoredResult<T, R> {

    val lockResult: LockedStoredResult<T, R>? = tryToLock(key = getLockKey(stored, repo), timeout = timeout) {
        // Reload the stored
        @Suppress("UNCHECKED_CAST")
        val reloaded: Stored<X> = (repo.findById(stored._id) as? Stored<X>)
            ?: return@tryToLock LockedStoredResult(value = stored, result = null, success = false)
        // Create the locked
        val locked: LockedStored<X> = LockedStored(reloaded)
        // Process the handler
        val result = handler(locked)
        // Was the locked changed
        val updated = if (locked.isModified) {
            // Stored the modified value
            repo.save(locked.stored)
        } else {
            // return result
            locked.stored
        }
        // return
        LockedStoredResult(value = updated, result = result, success = true)
    }

    return lockResult ?: LockedStoredResult(value = stored, result = null, success = false)
}

@PublishedApi
internal fun <T : Any> getLockKey(stored: Stored<T>, repo: Repository<T>): String {
    return "lock-stored-${stored._id}-in-${repo.name}"
}
