package io.peekandpoke.funktor.saas

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.kontainer.KontainerBuilder

abstract class OrgMembersStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()
    abstract fun FunktorSaasBuilder.configureSaas()

    private suspend fun setup(): Pair<OrgsStorage, OrgMembersStorage> {
        val kontainer = createSaasTestContainer(
            configureKontainer = { configureKontainer() },
            configureSaas = { configureSaas() },
        )
        val orgs = kontainer.get(OrgsStorage::class).also { it.clear() }
        val members = kontainer.get(OrgMembersStorage::class).also { it.clear() }
        return orgs to members
    }

    init {
        "add() a membership, then find it by user, by org and by (org, user)" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))

            val m = members.add(org = acme, userId = "b2b_users/u1", roles = setOf("owner"))

            m.value().userId shouldBe "b2b_users/u1"
            m.value().roles shouldBe setOf("owner")
            // The org ref carries the canonical _id (OrgAware contract) and its _key.
            m.value().org._id shouldBe acme._id
            m.value().org._key shouldBe acme._key

            members.findByUser("b2b_users/u1") shouldHaveSize 1
            members.findByOrg(acme.asRef) shouldHaveSize 1
            members.findByOrgAndUser(acme.asRef, "b2b_users/u1").shouldNotBeNull()
            members.findByOrgAndUser(acme.asRef, "b2b_users/does-not-exist") shouldBe null
        }

        "findByUser returns every org the user belongs to; findByOrg every member of an org" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val globex = orgs.create(Organisation(slug = "globex", name = "Globex"))

            members.add(acme, "b2b_users/u1", roles = setOf("owner"))
            members.add(globex, "b2b_users/u1", roles = setOf("member"))
            members.add(acme, "b2b_users/u2", roles = setOf("admin"))

            members.findByUser("b2b_users/u1").map { it.value().org._key }.toSet() shouldBe
                    setOf(acme._key, globex._key)
            // Pin the userId scoping: u2 ALSO has an acme row, so a dropped userId filter would return
            // 3 rows for u1 — the size + the u2-only check catch a cross-user leak on the login path.
            members.findByUser("b2b_users/u1") shouldHaveSize 2
            members.findByUser("b2b_users/u2").map { it.value().org._key } shouldBe listOf(acme._key)
            members.findByOrg(acme.asRef).map { it.value().userId }.toSet() shouldBe
                    setOf("b2b_users/u1", "b2b_users/u2")
            members.findByOrg(globex.asRef) shouldHaveSize 1

            // findByOrgAndUser resolves the row for THAT org specifically — not the user's other-org row.
            members.findByOrgAndUser(globex.asRef, "b2b_users/u1").shouldNotBeNull().value().let {
                it.org._key shouldBe globex._key
                it.roles shouldBe setOf("member")
            }
        }

        "save() persists a modified membership (roles change), keeping (org, userId)" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val m = members.add(acme, "b2b_users/u1", roles = setOf("member"))

            members.save(m.modify { it.copy(roles = setOf("owner", "admin")) })

            val reread = members.findByOrgAndUser(acme.asRef, "b2b_users/u1").shouldNotBeNull().value()
            reread.roles shouldBe setOf("owner", "admin")
            reread.userId shouldBe "b2b_users/u1"
            reread.org._key shouldBe acme._key
        }

        "(org, userId) is unique — a second row for the same pair is rejected" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            members.add(acme, "b2b_users/u1", roles = setOf("owner"))

            shouldThrowAny {
                members.add(acme, "b2b_users/u1", roles = setOf("admin"))
            }
        }

        "the same user may belong to two different orgs (unique is on the PAIR, not the user)" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val globex = orgs.create(Organisation(slug = "globex", name = "Globex"))

            members.add(acme, "b2b_users/u1", roles = setOf("owner"))
            members.add(globex, "b2b_users/u1", roles = setOf("member"))

            members.findByUser("b2b_users/u1") shouldHaveSize 2
        }

        "remove() deletes only the targeted membership" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val m1 = members.add(acme, "b2b_users/u1", roles = setOf("owner"))
            members.add(acme, "b2b_users/u2", roles = setOf("member"))

            members.remove(m1)

            members.findByUser("b2b_users/u1") shouldHaveSize 0
            // The other member survives — remove targets one row, not the collection.
            members.findByOrg(acme.asRef).map { it.value().userId } shouldBe listOf("b2b_users/u2")
        }
    }
}
