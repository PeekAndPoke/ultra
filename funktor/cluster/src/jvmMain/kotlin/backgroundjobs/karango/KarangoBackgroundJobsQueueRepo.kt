package de.peekandpoke.ktorfx.cluster.backgroundjobs.karango

import de.peekandpoke.karango.aql.ASC
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.LTE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.RETURN_COUNT
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.BackgroundJobQueued
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.dataHash
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.dueAt
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.state
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.type
import de.peekandpoke.ktorfx.core.fixtures.RepoFixtureLoader
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.slumber.ts

class KarangoBackgroundJobsQueueRepo(
    driver: KarangoDriver,
    repoName: String,
) :
    EntityRepository<BackgroundJobQueued>(name = repoName, kType(), driver),
    BackgroundJobs.Queue.Vault.Repo {

    /**
     * The fixtures are only here to clear the database collection
     */
    class Fixtures(repo: KarangoBackgroundJobsQueueRepo) : RepoFixtureLoader<BackgroundJobQueued>(repo)

    override fun IndexBuilder<BackgroundJobQueued>.buildIndexes() {
        this.persistentIndex {
            field { dueAt.ts }
        }

        persistentIndex {
            field { type }
            field { dataHash }
        }
    }

    override suspend fun findAllSorted(page: Int?, epp: Int?): Cursor<Stored<BackgroundJobQueued>> = find {
//        queryOptions {
//            it.count(true).fullCount(true)
//        }

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

    override suspend fun findNextDue(due: MpInstant): Stored<BackgroundJobQueued>? = findFirst {
        FOR(repo) {
            val dueAt = it.dueAt.ts

            FILTER(dueAt LTE due.toEpochMillis())
            SORT(dueAt.ASC)
            LIMIT(1)

            RETURN(it)
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
}
