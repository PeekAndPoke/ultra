package io.peekandpoke.funktor.cluster.backgroundjobs.karango

import com.arangodb.ArangoDBException
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dataHash
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dedupeKey
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dueAt
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.state
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.type
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.karango.aql.ASC
import io.peekandpoke.karango.aql.AqlExpression
import io.peekandpoke.karango.aql.AqlValueExpr
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.LTE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.RETURN_COUNT
import io.peekandpoke.karango.aql.RETURN_NEW
import io.peekandpoke.karango.aql.UPDATE
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.aql.ts
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored

class KarangoBackgroundJobsQueueRepo(
    driver: KarangoDriver,
    repoName: String,
) : EntityRepository<BackgroundJobQueued>(
    name = repoName,
    storedType = kType(),
    driver = driver
), BackgroundJobs.Queue.Vault.Repo {

    /**
     * The fixtures are only here to clear the database collection
     */
    class Fixtures(repo: KarangoBackgroundJobsQueueRepo) : RepoFixtureLoader<BackgroundJobQueued>(repo)

    override fun KarangoIndexBuilder<BackgroundJobQueued>.buildIndexes() {
        this.persistentIndex {
            field { dueAt.ts }
            field { state }
        }

        persistentIndex {
            field { type }
            field { dataHash }
        }

        // Sparse unique index that backs queueIfNotPresent's atomic dedupe across JVMs.
        // create()-path jobs leave dedupeKey null and are excluded from the index entirely
        // (ArangoDB sparse semantics treat null and missing as both "not indexed").
        persistentIndex {
            field { dedupeKey }
            options {
                unique(true)
                sparse(true)
            }
        }
    }

    override suspend fun findAllSorted(page: Int?, epp: Int?): Cursor<Stored<BackgroundJobQueued>> = find {
        queryOptions {
            it.count(true).fullCount(true)
        }

        FOR(repo) {
            SORT(it.dueAt.ts.ASC)

            if (page != null && epp != null) {
                PAGE(page = page, epp = epp)
            }

            RETURN(it)
        }
    }

    override suspend fun findAllDueSorted(due: MpInstant): Cursor<Stored<BackgroundJobQueued>> = find {
        FOR(repo) {
            val dueAt = it.dueAt.ts

            FILTER(dueAt LTE due.toEpochMillis())
            SORT(dueAt.ASC)

            RETURN(it)
        }
    }

    override suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>? = findFirst {
        FOR(repo) {
            val dueAt = it.dueAt.ts

            FILTER(dueAt LTE due.toEpochMillis())
            FILTER(it.state EQ BackgroundJobQueued.State.WAITING)
            SORT(dueAt.ASC)
            LIMIT(1)
            // Atomically transition state and clear dedupeKey so a new queueIfNotPresent for
            // the same (type, dataHash) can succeed while this one runs. Sparse index treats
            // null as not-indexed in ArangoDB, freeing the slot.
            UPDATE(it, repo) {
                put({ state }) {
                    BackgroundJobQueued.State.PROCESSING.aql
                }
                put({ dedupeKey }) {
                    // KSP-generated path is typed as String even though dedupeKey is String?,
                    // so we have to cast a null literal to AqlExpression<String> to fit the
                    // builder. ArangoDB sparse index excludes null values, freeing the slot.
                    @Suppress("UNCHECKED_CAST")
                    AqlValueExpr.Null() as AqlExpression<String>
                }
            }

            RETURN_NEW(it)
        }
    }

    override suspend fun hasWaitingByTypeAndDataHash(type: String, dataHash: Int): Boolean {
        val result = queryFirst {
            FOR(repo) {
                FILTER(it.type EQ type)
                FILTER(it.dataHash EQ dataHash)
                FILTER(it.state EQ BackgroundJobQueued.State.WAITING)

                RETURN_COUNT()
            }
        } as Int

        return result > 0
    }

    override suspend fun tryInsertWithDedupe(job: BackgroundJobQueued): Boolean {
        return try {
            insert(job)
            true
        } catch (e: Throwable) {
            // Karango wraps the driver error in KarangoQueryException; unwrap to find the
            // ArangoDBException with its errorNum. 1210 = unique constraint violated, which
            // means our sparse unique index on dedupeKey rejected the insert because another
            // JVM already enqueued the same (type, dataHash). Treat as no-op.
            val arango = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<ArangoDBException>()
                .firstOrNull()

            if (arango?.errorNum == UNIQUE_CONSTRAINT_VIOLATED) false else throw e
        }
    }

    private companion object {
        const val UNIQUE_CONSTRAINT_VIOLATED = 1210
    }
}
