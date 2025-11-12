package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored

class AuthRecordStorage(
    adapter: Lazy<Adapter>,
) {
    interface OnAfterSave : Repository.Hooks.OnAfterSave<AuthRecord>

    interface Adapter {

        object Null : Adapter {
            override suspend fun <T : AuthRecord> createRecord(new: T): Stored<T> {
                error("Not yet implemented")
            }

            override suspend fun findLatestBy(realm: String, type: String, owner: String): Stored<AuthRecord>? {
                return null
            }
        }

        class Vault(val inner: Repo) : Adapter {
            interface Repo : Repository<AuthRecord> {
                suspend fun findLatestBy(realm: String, type: String, owner: String): Stored<AuthRecord>?
            }

            override suspend fun <T : AuthRecord> createRecord(new: T): Stored<T> {
                return inner.insert(new)
            }

            override suspend fun findLatestBy(realm: String, type: String, owner: String): Stored<AuthRecord>? {
                return inner.findLatestBy(realm, type, owner)
            }
        }

        suspend fun <T : AuthRecord> createRecord(new: T): Stored<T>

        suspend fun findLatestBy(realm: String, type: String, owner: String): Stored<AuthRecord>?
    }

    val adapter by adapter

    suspend fun <T : AuthRecord> create(
        create: AuthRecordStorage.() -> T,
    ): Stored<T> {
        return adapter.createRecord(
            create(this)
        )
    }

    suspend inline fun <reified T : AuthRecord> findLatestRecordBy(
        type: Polymorphic.TypedChild<T>,
        realm: String,
        owner: String,
    ): Stored<T>? {
        return adapter.findLatestBy(
            type = type.identifier,
            realm = realm,
            owner = owner,
        )?.castTyped()
    }
}
