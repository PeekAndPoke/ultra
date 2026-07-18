package io.peekandpoke.funktor.saas.storage.monko

import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.domain.slug
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.firstOrNull
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class MonkoOrgsRepo(
    name: String = "system_organisations",
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : OrgsStorage.Vault.Repo, MonkoRepository<Organisation>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    class Fixtures(repo: MonkoOrgsRepo) : RepoFixtureLoader<Organisation>(repo)

    override fun MonkoIndexBuilder<Organisation>.buildIndexes() {
        uniqueIndex {
            field { it.slug }
        }
    }

    override suspend fun findBySlug(slug: String): Stored<Organisation>? {
        val found = find { r ->
            filter(r.slug eq slug)
            limit(1)
        }
        return found.firstOrNull()
    }
}
