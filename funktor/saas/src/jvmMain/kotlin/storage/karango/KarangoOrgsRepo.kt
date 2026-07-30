package io.peekandpoke.funktor.saas.storage.karango

import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.domain.slug
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class KarangoOrgsRepo(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : OrgsStorage.Vault.Repo, EntityRepository<Organisation>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    class Fixtures(repo: KarangoOrgsRepo) : RepoFixtureLoader<Organisation>(repo)

    override fun KarangoIndexBuilder<Organisation>.buildIndexes() {
        persistentIndex {
            field { slug }

            options {
                unique(true)
            }
        }
    }

    override suspend fun findBySlug(slug: String): Stored<Organisation>? = findFirst {
        FOR(repo) { org ->
            FILTER(org.slug EQ slug)
            LIMIT(1)
            RETURN(org)
        }
    }
}
