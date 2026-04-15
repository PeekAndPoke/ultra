package io.peekandpoke.funktor.cluster.backgroundjobs.monko

import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dataHash
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dedupeKey
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.dueAt
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.state
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.type
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.asc
import io.peekandpoke.monko.lang.dsl.combine
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.lte
import io.peekandpoke.monko.lang.dsl.setTo
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.monko.lang.dsl.unset
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored
import org.bson.Document

class MonkoBackgroundJobsQueueRepo(
    driver: MonkoDriver,
    repoName: String,
) : MonkoRepository<BackgroundJobQueued>(
    name = repoName,
    storedType = kType(),
    driver = driver,
), BackgroundJobs.Queue.Vault.Repo {

    /**
     * The fixtures are only here to clear the database collection
     */
    class Fixtures(repo: MonkoBackgroundJobsQueueRepo) : RepoFixtureLoader<BackgroundJobQueued>(repo)

    override fun MonkoIndexBuilder<BackgroundJobQueued>.buildIndexes() {
        persistentIndex {
            field { it.dueAt.ts }
            field { it.state }
        }

        persistentIndex {
            field { it.type }
            field { it.dataHash }
        }

        // Partial unique index that backs queueIfNotPresent's atomic dedupe across JVMs.
        // Only documents whose dedupeKey is a string participate. create()-path jobs leave
        // dedupeKey null (the codec serializes the absent default as null) so they are
        // excluded — sparse alone is insufficient because MongoDB sparse indexes still
        // include documents where the field is present with a null value.
        uniqueIndex {
            field { it.dedupeKey }
            partial(Document(repoExpr.dedupeKey.toFieldPath(), Document("\$type", "string")))
        }
    }

    override suspend fun findAllSorted(page: Int?, epp: Int?): Cursor<Stored<BackgroundJobQueued>> = find { r ->
        sort(r.dueAt.ts.asc)

        if (page != null && epp != null) {
            skip(page * epp)
            limit(epp)
        }
    }

    override suspend fun findAllDueSorted(due: MpInstant): Cursor<Stored<BackgroundJobQueued>> = find { r ->
        filter(r.dueAt.ts lte due.toEpochMillis())
        sort(r.dueAt.ts.asc)
    }

    override suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>? {
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        val filter = and(
            repoExpr.dueAt.ts lte due.toEpochMillis(),
            Filters.eq(repoExpr.state.toFieldPath(), BackgroundJobQueued.State.WAITING.name),
        )

        // unset (not "set null") is required: MongoDB sparse indexes only exclude documents
        // where the field is absent — null values still occupy a slot and would block re-enqueue.
        val update = combine(
            repoExpr.state setTo BackgroundJobQueued.State.PROCESSING,
            repoExpr.dedupeKey.unset(),
        )

        val options = FindOneAndUpdateOptions()
            .returnDocument(ReturnDocument.AFTER)
            .sort(Document(repoExpr.dueAt.ts.toFieldPath(), 1))

        val result = coll.findOneAndUpdate(filter, update, options)

        return result?.let { doc ->
            val key = "" + doc["_id"]

            @Suppress("UNCHECKED_CAST")
            val value = driver.codec.awake(storedType.type, doc) as BackgroundJobQueued

            Stored(
                _id = "$name/$key",
                _key = key,
                _rev = "",
                value = value,
            )
        }
    }

    override suspend fun hasWaitingByTypeAndDataHash(type: String, dataHash: Int): Boolean {
        val found = find { r ->
            filter(
                and(
                    r.type eq type,
                    r.dataHash eq dataHash,
                    Filters.eq(repoExpr.state.toFieldPath(), BackgroundJobQueued.State.WAITING.name),
                )
            )
            limit(1)
        }

        return found.count > 0
    }

    override suspend fun tryInsertWithDedupe(job: BackgroundJobQueued): Boolean {
        return try {
            insert(job)
            true
        } catch (e: MongoWriteException) {
            // Code 11000 = duplicate key — sparse unique index on dedupeKey rejected the insert
            // because another JVM already enqueued the same (type, dataHash). Treat as no-op.
            if (e.error.code == DUPLICATE_KEY_ERROR_CODE) false else throw e
        }
    }

    private companion object {
        const val DUPLICATE_KEY_ERROR_CODE = 11000
    }
}
