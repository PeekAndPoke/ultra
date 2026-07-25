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
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.UserId

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

            val m = members.add(org = acme, userId = UserId("b2b_users/u1"), roles = setOf("owner"))

            m.value().userId shouldBe UserId("b2b_users/u1")
            m.value().roles shouldBe setOf("owner")
            // The org ref carries the canonical _id (OrgAware contract) and its _key.
            m.value().org._id shouldBe acme._id
            m.value().org._key shouldBe acme._key

            members.findByUser(UserId("b2b_users/u1")) shouldHaveSize 1
            members.findByOrg(acme.asRef) shouldHaveSize 1
            members.findByOrgAndUser(acme.asRef, UserId("b2b_users/u1")).shouldNotBeNull()
            members.findByOrgAndUser(acme.asRef, UserId("b2b_users/does-not-exist")) shouldBe null
        }

        "findByUser returns every org the user belongs to; findByOrg every member of an org" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val globex = orgs.create(Organisation(slug = "globex", name = "Globex"))

            members.add(acme, UserId("b2b_users/u1"), roles = setOf("owner"))
            members.add(globex, UserId("b2b_users/u1"), roles = setOf("member"))
            members.add(acme, UserId("b2b_users/u2"), roles = setOf("admin"))

            members.findByUser(UserId("b2b_users/u1")).map { it.value().org._key }.toSet() shouldBe
                    setOf(acme._key, globex._key)
            // Pin the userId scoping: u2 ALSO has an acme row, so a dropped userId filter would return
            // 3 rows for u1 — the size + the u2-only check catch a cross-user leak on the login path.
            members.findByUser(UserId("b2b_users/u1")) shouldHaveSize 2
            members.findByUser(UserId("b2b_users/u2")).map { it.value().org._key } shouldBe listOf(acme._key)
            members.findByOrg(acme.asRef).map { it.value().userId }.toSet() shouldBe
                    setOf(UserId("b2b_users/u1"), UserId("b2b_users/u2"))
            members.findByOrg(globex.asRef) shouldHaveSize 1

            // findByOrgAndUser resolves the row for THAT org specifically — not the user's other-org row.
            members.findByOrgAndUser(globex.asRef, UserId("b2b_users/u1")).shouldNotBeNull().value().let {
                it.org._key shouldBe globex._key
                it.roles shouldBe setOf("member")
            }
        }

        "save() persists a modified membership (roles change), keeping (org, userId)" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val m = members.add(acme, UserId("b2b_users/u1"), roles = setOf("member"))

            members.save(m.modify { it.copy(roles = setOf("owner", "admin")) })

            val reread = members.findByOrgAndUser(acme.asRef, UserId("b2b_users/u1")).shouldNotBeNull().value()
            reread.roles shouldBe setOf("owner", "admin")
            reread.userId shouldBe UserId("b2b_users/u1")
            reread.org._key shouldBe acme._key
        }

        "(org, userId) is unique — a second row for the same pair is rejected" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            members.add(acme, UserId("b2b_users/u1"), roles = setOf("owner"))

            shouldThrowAny {
                members.add(acme, UserId("b2b_users/u1"), roles = setOf("admin"))
            }
        }

        "the same user may belong to two different orgs (unique is on the PAIR, not the user)" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val globex = orgs.create(Organisation(slug = "globex", name = "Globex"))

            members.add(acme, UserId("b2b_users/u1"), roles = setOf("owner"))
            members.add(globex, UserId("b2b_users/u1"), roles = setOf("member"))

            members.findByUser(UserId("b2b_users/u1")) shouldHaveSize 2
        }

        "remove() soft-deletes: excluded from reads, sibling survives, (org,userId) slot retained" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val m1 = members.add(acme, UserId("b2b_users/u1"), roles = setOf("owner"))
            members.add(acme, UserId("b2b_users/u2"), roles = setOf("member"))

            members.remove(m1)

            // Excluded from EVERY read (findByUser/findByOrg/findByOrgAndUser apply the notDeleted filter).
            members.findByUser(UserId("b2b_users/u1")) shouldHaveSize 0
            members.findByOrgAndUser(acme.asRef, UserId("b2b_users/u1")) shouldBe null
            // The other member survives — remove targets one row, not the collection.
            members.findByOrg(acme.asRef).map { it.value().userId } shouldBe listOf(UserId("b2b_users/u2"))
            // Soft-delete (not hard): the (org,userId) row is retained as an audit record, so a raw
            // add() of the SAME pair still collides with the unique index (a hard delete would free the
            // slot). Reactivation is done at the LEAF via findByOrgAndUserIncludingDeleted + save(), NOT
            // by add() — see the findByOrgAndUserIncludingDeleted test below.
            shouldThrowAny { members.add(acme, UserId("b2b_users/u1"), roles = setOf("member")) }
        }

        "findByOrgAndUserIncludingDeleted sees a soft-deleted row that findByOrgAndUser hides" {
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            val m = members.add(acme, UserId("b2b_users/u1"), roles = setOf("member"))

            // Active: both reads return the row.
            members.findByOrgAndUser(acme.asRef, UserId("b2b_users/u1")).shouldNotBeNull()
            members.findByOrgAndUserIncludingDeleted(acme.asRef, UserId("b2b_users/u1")).shouldNotBeNull()

            members.remove(m) // soft-delete

            // The notDeleted read hides it; the including-deleted read still returns the retained slot
            // (carrying the soft-delete marker) — this is what the leaf reactivation path relies on.
            members.findByOrgAndUser(acme.asRef, UserId("b2b_users/u1")) shouldBe null
            members.findByOrgAndUserIncludingDeleted(acme.asRef, UserId("b2b_users/u1")).shouldNotBeNull().value().let {
                it.userId shouldBe UserId("b2b_users/u1")
                it.softDelete.shouldNotBeNull()
            }

            // An absent pair is null on the including-deleted read too.
            members.findByOrgAndUserIncludingDeleted(acme.asRef, UserId("b2b_users/nope")) shouldBe null
        }

        "sessionMembershipsOf maps stored rows to session memberships (orgId=_key, roles, branchIds)" {
            // Pins the storage->session seam the login path (getMemberships) depends on: a regression
            // dropping roles/branchIds here would strip org roles from the JWT yet keep the
            // org-selection acceptance tests (which assert only org slugs) green.
            val (orgs, members) = setup()
            val acme = orgs.create(Organisation(slug = "acme", name = "Acme"))
            members.add(acme, UserId("b2b_users/u1"), roles = setOf("owner", "admin"), branchIds = setOf("berlin"))

            members.sessionMembershipsOf(UserId("b2b_users/u1")) shouldBe setOf(
                OrgMembership(orgId = acme._key, branchIds = setOf("berlin"), roles = setOf("owner", "admin")),
            )
            members.sessionMembershipsOf(UserId("b2b_users/unknown")) shouldBe emptySet()
        }
    }
}
