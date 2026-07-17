package io.peekandpoke.funktor.saas

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.kontainer.KontainerBuilder

abstract class OrgsStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()
    abstract fun FunktorSaasBuilder.configureSaas()

    private suspend fun createStorage(): OrgsStorage {
        val kontainer = createSaasTestContainer(
            configureKontainer = { configureKontainer() },
            configureSaas = { configureSaas() },
        )
        return kontainer.get(OrgsStorage::class).also { it.clear() }
    }

    init {
        "create() an organisation with branches, then find it by id and by slug" {
            val storage = createStorage()

            val created = storage.create(
                Organisation(
                    slug = "acme-hotels",
                    name = "Acme Hotels",
                    branches = listOf(
                        Organisation.Branch(id = "berlin", slug = "berlin", name = "Berlin Site"),
                        Organisation.Branch(id = "munich", slug = "munich", name = "Munich Site"),
                    ),
                )
            )

            val byId = storage.findById(created._id)
            byId.shouldNotBeNull()

            val org = byId.value()
            org.name shouldBe "Acme Hotels"
            org.status shouldBe OrgStatus.Active
            org.branches shouldHaveSize 2
            org.branches.map { it.slug } shouldBe listOf("berlin", "munich")

            val bySlug = storage.findBySlug("acme-hotels")
            bySlug.shouldNotBeNull()
            bySlug._id shouldBe created._id
        }

        "findBySlug() returns null for an unknown slug" {
            val storage = createStorage()

            storage.findBySlug("does-not-exist") shouldBe null
        }

        "findAll() returns all organisations" {
            val storage = createStorage()

            storage.create(Organisation(slug = "org-a", name = "Org A"))
            storage.create(Organisation(slug = "org-b", name = "Org B"))

            storage.findAll() shouldHaveSize 2
        }

        "slug must be unique" {
            val storage = createStorage()

            storage.create(Organisation(slug = "dupe", name = "First"))

            shouldThrowAny {
                storage.create(Organisation(slug = "dupe", name = "Second"))
            }
        }
    }
}
