package de.peekandpoke.ktorfx.cluster.backgroundjobs.karango

import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.BackgroundJobArchived
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.archivedAt
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.expiresAt
import de.peekandpoke.ktorfx.core.fixtures.RepoFixtureLoader
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Stored

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

    override fun IndexBuilder<BackgroundJobArchived>.buildIndexes() {
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
