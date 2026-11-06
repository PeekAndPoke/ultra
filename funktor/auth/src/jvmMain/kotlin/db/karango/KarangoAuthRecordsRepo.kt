package io.peekandpoke.funktor.auth.db.karango

import com.arangodb.ArangoDBException
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.expiresAt
import io.peekandpoke.funktor.auth.domain.ownerId
import io.peekandpoke.funktor.auth.domain.realm
import io.peekandpoke.funktor.auth.domain.TokenHash
import io.peekandpoke.funktor.auth.domain.token
import io.peekandpoke.funktor.auth.domain.tokenHash
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.karango.KarangoQueryException
import io.peekandpoke.karango.aql.AqlPrinter
import io.peekandpoke.karango.aql.AqlTerminalExpr
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
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
import kotlinx.coroutines.delay
import kotlin.random.Random

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

    private companion object {
        /** Arango's `ERROR_ARANGO_CONFLICT`: the document was written by another transaction meanwhile. */
        const val WRITE_WRITE_CONFLICT = 1200

        /** Runs of one write before a conflict is thrown; each re-run sees the writes that beat the last. */
        const val CONFLICT_ATTEMPTS = 8
    }

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

        // The one-shot lookup by its hash.
        persistentIndex {
            field { tokenHash }
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

    override suspend fun findByTokenHash(realm: RealmId, type: String, hash: TokenHash): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.tokenHash EQ hash)

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
        val result = retryOnConflict {
            query {
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
        }

        return RemoveResult(count = result.count, query = result.query)
    }

    override suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult {
        val result = retryOnConflict {
            query {
                FOR(repo) { r ->
                    FILTER(r.realm EQ realm)
                    FILTER(r.ownerId EQ owner)
                    if (exceptType != null) {
                        FILTER(r._type NE exceptType)
                    }
                    REMOVE(r) IN repo
                }
            }
        }

        return RemoveResult(count = result.count, query = result.query)
    }

    override suspend fun findCreatedBefore(
        realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
    ): List<Stored<AuthRecord>> {
        // Hand-written: `claimedBy` lives on one record type only, so the typed DSL has no path to it.
        val cursor = find {
            Aql(repo) { p ->
                p.append("FOR r IN ").append(repo).appendLine()
                p.indent {
                    append("FILTER r._type == ").value("type", type).append(" && r.realm == ").value("realm", realm.value)
                        .appendLine()
                    append("FILTER r.createdAt.ts < ").value("before", before.toEpochMillis())
                    if (orClaimedBy != null) append(" || r.claimedBy == ").value("claimedBy", orClaimedBy)
                    appendLine()
                    if (after != null) append("FILTER r.createdAt.ts > ").value("after", after.toEpochMillis()).appendLine()
                    append("SORT r.createdAt.ts ASC").appendLine()
                    append("LIMIT ").value("limit", limit).appendLine()
                    append("RETURN r").appendLine()
                }
            }
        }
        return cursor.toList()
    }

    override suspend fun <T : AuthRecord> replaceIfClaimedBy(
        stored: Stored<T>, expected: String?, expectedResends: Int?,
    ): Stored<T>? {
        @Suppress("UNCHECKED_CAST")
        return retryOnConflict {
            findFirst {
                IfClaimedBy(stored._key, expected, repo, expectedResends = expectedResends) { p -> p.value("v", stored) }
            }
        } as Stored<T>?
    }

    override suspend fun removeIfClaimedBy(id: String, expected: String?): RemoveResult {
        val removed = retryOnConflict {
            find { IfClaimedBy(id.substringAfterLast("/"), expected, repo, replaceWith = null) }.toList()
        }

        return RemoveResult(count = removed.size.toLong(), query = null)
    }

    /**
     * Runs a write [block] again when Arango aborted it with a write-write conflict (1200): another write of the same
     * document committed meanwhile. The aborted statement wrote nothing, and its FILTER never saw the other's result,
     * so the re-run decides against the committed document — a conditional write then loses exactly when its
     * condition fails, as on Mongo. Not a 500 for the slower of two re-sends, claims or removes (ULTRA-00130). A
     * conflict that persists over [CONFLICT_ATTEMPTS] runs is thrown. A short random pause before each re-run keeps
     * the writers that collided from colliding again in lockstep.
     */
    private suspend inline fun <R> retryOnConflict(block: () -> R): R {
        var attempt = 1

        while (true) {
            try {
                return block()
            } catch (e: KarangoQueryException) {
                val conflict = (e.cause as? ArangoDBException)?.errorNum == WRITE_WRITE_CONFLICT
                if (!conflict || attempt >= CONFLICT_ATTEMPTS) throw e
                delay(Random.nextLong(1, 5L * attempt + 1))
                attempt++
            }
        }
    }

    /** A hand-written AQL query over [repo], returning its documents. */
    private class Aql<T : Any>(
        private val repo: KarangoRepository<T>,
        private val printer: (AqlPrinter) -> Unit,
    ) : AqlTerminalExpr<T> {

        override fun innerType(): TypeRef<T> = repo.storedType

        override fun getType() = repo.getType()

        override fun print(p: AqlPrinter) = printer(p)
    }

    /**
     * One AQL statement on the document [key] while it exists and its `claimedBy` equals [expected] (null:
     * unclaimed — a missing attribute reads as null, so other record types qualify) and, when given, its `resends`
     * equals [expectedResends]: `REPLACE` it with [replaceWith], or `REMOVE` it when that is null. Atomic, unlike a
     * read followed by a write.
     */
    private class IfClaimedBy<T : Any>(
        private val key: String,
        private val expected: String?,
        private val repo: KarangoRepository<T>,
        private val expectedResends: Int? = null,
        private val replaceWith: ((AqlPrinter) -> Unit)?,
    ) : AqlTerminalExpr<T> {

        override fun innerType(): TypeRef<T> = repo.storedType

        override fun getType() = repo.getType()

        override fun print(p: AqlPrinter) {
            p.append("FOR doc IN ").append(repo)
                .append(" FILTER doc._key == ").value("key", key)
                .append(" && doc.claimedBy == ").value("expected", expected)
            if (expectedResends != null) p.append(" && doc.resends == ").value("resends", expectedResends)
            p.appendLine()
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
