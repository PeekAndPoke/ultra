package io.peekandpoke.funktor.cluster.backgroundjobs.monko

import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobArchived
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.archivedAt
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.expiresAt
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored

class MonkoBackgroundJobsArchiveRepo(
    driver: MonkoDriver,
    repoName: String,
) : MonkoRepository<BackgroundJobArchived>(
    name = repoName,
    storedType = kType(),
    driver = driver,
), BackgroundJobs.Archive.Vault.Repo {

    /**
     * The fixtures are only here to clear the database collection
     */
    class Fixtures(repo: MonkoBackgroundJobsArchiveRepo) : RepoFixtureLoader<BackgroundJobArchived>(repo)

    override fun MonkoIndexBuilder<BackgroundJobArchived>.buildIndexes() {
        ttlIndex {
            field { it.expiresAt }
            expireAfter(0)
        }

        persistentIndex {
            field { it.archivedAt.ts }
        }
    }

    override suspend fun findAllNewestFirst(page: Int?, epp: Int?): Cursor<Stored<BackgroundJobArchived>> = find { r ->
        sort(r.archivedAt.ts.desc)

        if (page != null && epp != null) {
            skip(page * epp)
            limit(epp)
        }
    }
}
