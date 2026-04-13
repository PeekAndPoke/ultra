package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored

class AuthRecordStorage(
    adapter: Lazy<Adapter>,
    kronos: Lazy<Kronos>,
) {
    interface OnAfterSave : Repository.Hooks.OnAfterSave<AuthRecord>

    interface Adapter {

        object Null : Adapter {
            override suspend fun <T : AuthRecord> createRecord(new: T): Stored<T> {
                error("Not yet implemented")
            }

            override suspend fun removeById(id: String): RemoveResult {
                return RemoveResult.empty
            }

            override suspend fun removeAll(): RemoveResult {
                return RemoveResult.empty
            }

            override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
                return null
            }

            override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
                return null
            }
        }

        class Vault(val inner: Repo) : Adapter {
            interface Repo : Repository<AuthRecord> {

                suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>?

                suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>?
            }

            override suspend fun removeById(id: String): RemoveResult {
                return inner.remove(id)
            }

            override suspend fun removeAll(): RemoveResult {
                return inner.removeAll()
            }

            override suspend fun <T : AuthRecord> createRecord(new: T): Stored<T> {
                return inner.insert(new)
            }

            override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
                return inner.findLatest(realm, type, owner)
            }

            override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
                return inner.findByToken(realm, type, token)
            }
        }

        suspend fun <T : AuthRecord> createRecord(new: T): Stored<T>

        suspend fun removeById(id: String): RemoveResult

        suspend fun removeAll(): RemoveResult

        suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>?

        suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>?
    }

    val adapter by adapter
    val kronos by kronos

    /** Create a new auth record returned by [block] */
    suspend fun <T : AuthRecord> create(block: AuthRecordStorage.() -> T): Stored<T> {
        return create(block(this))
    }

    /** Create a [new] auth record */
    suspend fun <T : AuthRecord> create(new: T): Stored<T> {
        return adapter.createRecord(new)
    }

    /** Remove an auth record by its [id] */
    suspend fun removeById(id: String): RemoveResult {
        return adapter.removeById(id)
    }

    /** Find the latest auth record of type [type], [realm] and [owner] */
    suspend inline fun <reified T : AuthRecord> findLatestRecordBy(
        type: Polymorphic.TypedChild<T>, realm: String, owner: String,
    ): Stored<T>? {
        return adapter.findLatest(type = type.identifier, realm = realm, owner = owner)
            ?.takeIf { it.hasNotExpired() }
            ?.castTyped()
    }

    /** Find the latest auth record of type [type], [realm] and [token] */
    suspend inline fun <reified T : AuthRecord> findByToken(
        type: Polymorphic.TypedChild<T>, realm: String, token: String,
    ): Stored<T>? {
        return adapter
            .findByToken(type = type.identifier, realm = realm, token = token)
            ?.takeIf { it.hasNotExpired() }
            ?.castTyped()
    }

    /** Checks if the given record has not yet expired */
    suspend fun <T : AuthRecord> Stored<T>.hasNotExpired(): Boolean {
        val expiresAt = resolve().expiresAt ?: return true

        return expiresAt > kronos.secondsNow()
    }

    /** Checks if the given record has expired */
    suspend fun <T : AuthRecord> Stored<T>.hasExpired(): Boolean {
        return hasNotExpired().not()
    }
}
