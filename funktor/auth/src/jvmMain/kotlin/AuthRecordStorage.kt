package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored

class AuthRecordStorage(adapter: Lazy<Adapter>, kronos: Lazy<Kronos>) {
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

            override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? {
                return null
            }

            override suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>? {
                return null
            }

            override suspend fun findAllByOwner(realm: RealmId, type: String, owner: UserId): List<Stored<AuthRecord>> {
                return emptyList()
            }

            override suspend fun removeAllByOwner(
                realm: RealmId, type: String, owner: UserId, exceptId: String?,
            ): RemoveResult {
                return RemoveResult.empty
            }

            override suspend fun <T : AuthRecord> replaceRecordIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>? {
                return null
            }

            override suspend fun removeRecordIfClaimedBy(id: String, expected: String?): RemoveResult {
                return RemoveResult.empty
            }

            override suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult {
                return RemoveResult.empty
            }

            override suspend fun findCreatedBefore(
                realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
            ): List<Stored<AuthRecord>> {
                return emptyList()
            }
        }

        class Vault(val inner: Repo) : Adapter {
            interface Repo : Repository<AuthRecord> {

                suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>?

                suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>?

                suspend fun findAllByOwner(realm: RealmId, type: String, owner: UserId): List<Stored<AuthRecord>>

                suspend fun removeAllByOwner(
                    realm: RealmId, type: String, owner: UserId, exceptId: String?,
                ): RemoveResult

                suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult

                suspend fun findCreatedBefore(
                    realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
                ): List<Stored<AuthRecord>>

                suspend fun <T : AuthRecord> replaceIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>?

                suspend fun removeIfClaimedBy(id: String, expected: String?): RemoveResult
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

            override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? {
                return inner.findLatest(realm, type, owner)
            }

            override suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>? {
                return inner.findByToken(realm, type, token)
            }

            override suspend fun findAllByOwner(realm: RealmId, type: String, owner: UserId): List<Stored<AuthRecord>> {
                return inner.findAllByOwner(realm, type, owner)
            }

            override suspend fun removeAllByOwner(
                realm: RealmId, type: String, owner: UserId, exceptId: String?,
            ): RemoveResult {
                return inner.removeAllByOwner(realm, type, owner, exceptId)
            }

            override suspend fun <T : AuthRecord> replaceRecordIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>? {
                return inner.replaceIfClaimedBy(stored, expected)
            }

            override suspend fun removeRecordIfClaimedBy(id: String, expected: String?): RemoveResult {
                return inner.removeIfClaimedBy(id, expected)
            }

            override suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult {
                return inner.removeAllOfOwner(realm, owner, exceptType)
            }

            override suspend fun findCreatedBefore(
                realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
            ): List<Stored<AuthRecord>> {
                return inner.findCreatedBefore(realm, type, before, orClaimedBy, after, limit)
            }
        }

        suspend fun <T : AuthRecord> createRecord(new: T): Stored<T>

        suspend fun removeById(id: String): RemoveResult

        suspend fun removeAll(): RemoveResult

        suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>?

        suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>?

        /** Returns all records of [type] in [realm] owned by [owner], unsorted. */
        suspend fun findAllByOwner(realm: RealmId, type: String, owner: UserId): List<Stored<AuthRecord>>

        /** Removes all records of [type] in [realm] owned by [owner], optionally excluding [exceptId]. */
        suspend fun removeAllByOwner(
            realm: RealmId, type: String, owner: UserId, exceptId: String? = null,
        ): RemoveResult

        /**
         * Replaces [stored] only while a record with its `_id` exists AND its stored `claimedBy` equals [expected]
         * (null: unclaimed, which an absent field also is) — in one atomic statement, never inserting; null otherwise.
         */
        suspend fun <T : AuthRecord> replaceRecordIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>?

        /** Removes the record [id] only while its `claimedBy` equals [expected] — in one atomic statement. */
        suspend fun removeRecordIfClaimedBy(id: String, expected: String?): RemoveResult

        /** Removes every record of [owner] in [realm], of ANY type except [exceptType]. */
        suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String? = null): RemoveResult

        /**
         * Returns up to [limit] records of [type] in [realm] created before [before] OR, when given, claimed by
         * [orClaimedBy] at any age — and, when given, created after [after] — oldest first.
         */
        suspend fun findCreatedBefore(
            realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
        ): List<Stored<AuthRecord>>
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
        type: Polymorphic.TypedChild<T>, realm: RealmId, owner: UserId,
    ): Stored<T>? {
        return adapter.findLatest(type = type.identifier, realm = realm, owner = owner)
            ?.takeIf { it.hasNotExpired() }
            ?.castTyped()
    }

    /** Find the latest auth record of type [type], [realm] and [token] */
    suspend inline fun <reified T : AuthRecord> findByToken(
        type: Polymorphic.TypedChild<T>, realm: RealmId, token: String,
    ): Stored<T>? {
        return adapter
            .findByToken(type = type.identifier, realm = realm, token = token)
            ?.takeIf { it.hasNotExpired() }
            ?.castTyped()
    }

    /** List all non-expired records of [type] for [realm] / [owner]. */
    suspend inline fun <reified T : AuthRecord> findAllByOwner(
        type: Polymorphic.TypedChild<T>, realm: RealmId, owner: UserId,
    ): List<Stored<T>> {
        return adapter.findAllByOwner(type = type.identifier, realm = realm, owner = owner)
            .filter { it.hasNotExpired() }
            .mapNotNull { it.castTyped() }
    }

    /** Remove all records of [type] for [realm] / [owner], optionally keeping [exceptId]. */
    suspend inline fun <reified T : AuthRecord> removeAllByOwner(
        type: Polymorphic.TypedChild<T>, realm: RealmId, owner: UserId, exceptId: String? = null,
    ): RemoveResult {
        return adapter.removeAllByOwner(
            type = type.identifier, realm = realm, owner = owner, exceptId = exceptId,
        )
    }

    /**
     * Replaces [stored] only while it still exists and is unclaimed (see [AuthRecord.PendingActivation.claimedBy]),
     * stamping `updatedAt`; null otherwise. Atomic on both backends — the write for state another request may
     * have deleted or claimed meanwhile. Never inserts.
     */
    suspend fun <T : AuthRecord> replaceIfUnclaimed(stored: Stored<T>): Stored<T>? = replaceIfClaimedBy(stored, expected = null)

    /**
     * Replaces [stored] only while it still exists and its stored `claimedBy` equals [expected] (null: unclaimed),
     * stamping `updatedAt`; null otherwise. One atomic statement on both backends — e.g. a claim TAKEOVER, which
     * must lose if the claimant finished meanwhile.
     */
    suspend fun <T : AuthRecord> replaceIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>? {
        @Suppress("UNCHECKED_CAST")
        val stamped = stored.modify { it.withUpdatedAt(kronos.instantNow()) as T }

        return adapter.replaceRecordIfClaimedBy(stamped, expected)
    }

    /** Removes the record [id] only while it is unclaimed; atomic on both backends. True when THIS call removed it. */
    suspend fun removeIfUnclaimed(id: String): Boolean = removeIfClaimedBy(id, expected = null)

    /** Removes the record [id] only while its `claimedBy` equals [expected]; atomic. True when THIS call removed it. */
    suspend fun removeIfClaimedBy(id: String, expected: String?): Boolean {
        return adapter.removeRecordIfClaimedBy(id, expected).count == 1L
    }

    /** Remove every record of [owner] in [realm], of any type, optionally keeping those of [except]. */
    suspend fun removeAllOfOwner(
        realm: RealmId, owner: UserId, except: Polymorphic.TypedChild<*>? = null,
    ): RemoveResult {
        return adapter.removeAllOfOwner(realm = realm, owner = owner, exceptType = except?.identifier)
    }

    /**
     * Up to [limit] records of [type] in [realm] created before [before] — or claimed by [orClaimedBy] at any age —
     * and after [after] (to page on), oldest first, expired or not.
     */
    suspend inline fun <reified T : AuthRecord> findCreatedBefore(
        type: Polymorphic.TypedChild<T>,
        realm: RealmId,
        before: MpInstant,
        orClaimedBy: String? = null,
        after: MpInstant? = null,
        limit: Int,
    ): List<Stored<T>> {
        return adapter
            .findCreatedBefore(
                realm = realm, type = type.identifier, before = before, orClaimedBy = orClaimedBy, after = after,
                limit = limit,
            )
            .mapNotNull { it.castTyped() }
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
