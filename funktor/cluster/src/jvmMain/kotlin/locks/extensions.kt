package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlin.time.Duration

/** Wrapper around a [Stored] entity that was loaded under a global lock, enabling safe modify-and-save. */
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

    suspend fun resolve(): T = stored.resolve()

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

/** Result of a lock-reload-modify-save operation on a [Stored] entity. */
data class LockedStoredResult<T, out R>(
    val value: Stored<T>,
    val result: R?,
    val success: Boolean,
)

/** Acquires a lock on the stored entity, reloads it from the repo, applies [handler], and saves if modified. */
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
