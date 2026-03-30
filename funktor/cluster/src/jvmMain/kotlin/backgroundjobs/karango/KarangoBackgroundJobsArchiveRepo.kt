package io.peekandpoke.funktor.cluster.backgroundjobs.karango

import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobArchived
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.archivedAt
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.expiresAt
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored

class KarangoBackgroundJobsArchiveRepo(
    driver: KarangoDriver,
    repoName: String,
) :
    EntityRepository<BackgroundJobArchived>(name = repoName, kType(), driver),
    BackgroundJobs.Archive.Vault.Repo {

    /**
     * The fixtures are only here to clear the database collection
     */
    class Fixtures(repo: KarangoBackgroundJobsArchiveRepo) : RepoFixtureLoader<BackgroundJobArchived>(repo)

    override fun KarangoIndexBuilder<BackgroundJobArchived>.buildIndexes() {
        ttlIndex {
            field { expiresAt }
        }

        this.persistentIndex {
            field { archivedAt.property<Long>("ts") }
        }
    }

    override suspend fun findAllNewestFirst(page: Int?, epp: Int?): Cursor<Stored<BackgroundJobArchived>> = find {
//        queryOptions {
//            it.count(true).fullCount(true)
//        }

        FOR(repo) {
            SORT(it.archivedAt.property<Long>("ts").DESC)

            if (page != null && epp != null) {
                PAGE(page = page, epp = epp)
            }

            RETURN(it)
        }
    }
}
