package io.peekandpoke.funktor.auth.db.karango

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.expiresAt
import io.peekandpoke.funktor.auth.domain.ownerId
import io.peekandpoke.funktor.auth.domain.realm
import io.peekandpoke.funktor.auth.domain.token
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.karango.aql.AqlPrinter
import io.peekandpoke.karango.aql.AqlTerminalExpr
import io.peekandpoke.karango.aql.ASC
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GT
import io.peekandpoke.karango.aql.LT
import io.peekandpoke.karango.aql.NE
import io.peekandpoke.karango.aql.REMOVE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql._type
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.karango.vault.KarangoRepository
import io.peekandpoke.karango.vault._id
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.ts
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class KarangoAuthRecordsRepo(
    name: String = "system_auth_records",
    driver: KarangoDriver,
    onAfterSaves: List<AuthRecordStorage.OnAfterSave>,
    timestamped: TimestampedHook,
) : AuthRecordStorage.Adapter.Vault.Repo, EntityRepository<AuthRecord>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamped.onBeforeSave())
) {
    class Fixtures(repo: KarangoAuthRecordsRepo) : RepoFixtureLoader<AuthRecord>(repo)

    override fun KarangoIndexBuilder<AuthRecord>.buildIndexes() {
        persistentIndex {
            field { realm }
            field { ownerId }
            field { _type }
        }

        // The expiry sweep: markers of one realm by age.
        persistentIndex {
            field { realm }
            field { _type }
            field { createdAt.ts }
        }

        ttlIndex {
            field { expiresAt }
        }
    }

    override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.ownerId EQ owner)

                SORT(r.createdAt.ts.DESC)

                LIMIT(1)

                RETURN(r)
            }
        }
    }

    override suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.token EQ token)

                LIMIT(1)

                RETURN(r)
            }
        }
    }

    override suspend fun findAllByOwner(
        realm: RealmId, type: String, owner: UserId,
    ): List<Stored<AuthRecord>> {
        val cursor = find {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.ownerId EQ owner)
                SORT(r.createdAt.ts.DESC)
                RETURN(r)
            }
        }
        return cursor.toList()
    }

    override suspend fun removeAllByOwner(
        realm: RealmId, type: String, owner: UserId, exceptId: String?,
    ): RemoveResult {
        val result = query {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.ownerId EQ owner)
                if (exceptId != null) {
                    FILTER(r._id NE exceptId)
                }
                REMOVE(r) IN repo
            }
        }

        return RemoveResult(count = result.count, query = result.query)
    }

    override suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult {
        val result = query {
            FOR(repo) { r ->
                FILTER(r.realm EQ realm)
                FILTER(r.ownerId EQ owner)
                if (exceptType != null) {
                    FILTER(r._type NE exceptType)
                }
                REMOVE(r) IN repo
            }
        }

        return RemoveResult(count = result.count, query = result.query)
    }

    override suspend fun findCreatedBefore(
        realm: RealmId, type: String, before: MpInstant, after: MpInstant?, limit: Int,
    ): List<Stored<AuthRecord>> {
        val cursor = find {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.createdAt.ts LT before.toEpochMillis())
                if (after != null) {
                    FILTER(r.createdAt.ts GT after.toEpochMillis())
                }
                SORT(r.createdAt.ts.ASC)
                LIMIT(limit)
                RETURN(r)
            }
        }
        return cursor.toList()
    }

    override suspend fun <T : AuthRecord> replaceIfUnclaimed(stored: Stored<T>): Stored<T>? {
        @Suppress("UNCHECKED_CAST")
        return findFirst { IfUnclaimed(stored._key, repo) { p -> p.value("v", stored) } } as Stored<T>?
    }

    override suspend fun removeIfUnclaimed(id: String): RemoveResult {
        val removed = find { IfUnclaimed(id.substringAfterLast("/"), repo, replaceWith = null) }.toList()

        return RemoveResult(count = removed.size.toLong(), query = null)
    }

    /**
     * One AQL statement on the document [key] while it exists and is unclaimed: `REPLACE` it with [replaceWith],
     * or `REMOVE` it when that is null. Atomic, unlike a read followed by a write; a missing `claimedBy`
     * attribute reads as null, so other record types always qualify.
     */
    private class IfUnclaimed<T : Any>(
        private val key: String,
        private val repo: KarangoRepository<T>,
        private val replaceWith: ((AqlPrinter) -> Unit)?,
    ) : AqlTerminalExpr<T> {

        override fun innerType(): TypeRef<T> = repo.storedType

        override fun getType() = repo.getType()

        override fun print(p: AqlPrinter) {
            p.append("FOR doc IN ").append(repo)
                .append(" FILTER doc._key == ").value("key", key)
                .append(" && doc.claimedBy == null").appendLine()
            p.indent {
                when (replaceWith) {
                    null -> append("REMOVE doc IN ").append(repo).appendLine().append("RETURN OLD").appendLine()
                    else -> {
                        append("REPLACE doc WITH ")
                        replaceWith(this)
                        append(" IN ").append(repo).appendLine()
                        append("RETURN NEW").appendLine()
                    }
                }
            }
        }
    }
}
