package io.peekandpoke.funktor.saas

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored

/**
 * The [OrgMembersStorage.Null] contract (no DB): reads return empty/null, writes fail loudly. This
 * is the fail-closed default active until a backend is selected — a mis-wired backend must yield NO
 * memberships (no access), never silent success.
 */
class OrgMembersStorageNullSpec : FreeSpec({

    val storage = OrgMembersStorage.Null()

    val org = Stored(
        value = Organisation(slug = "acme", name = "Acme"),
        _id = "saas_organisations/acme",
        _key = "acme",
    )
    val member = Stored(
        value = OrgMember(org = org.asRef, userId = UserId("b2b_users/u1")),
        _id = "saas_org_members/m1",
        _key = "m1",
    )

    "reads return empty / null" {
        storage.findByUser(UserId("b2b_users/u1")) shouldBe emptyList()
        storage.findByOrg(org.asRef) shouldBe emptyList()
        storage.findByOrgAndUser(org.asRef, UserId("b2b_users/u1")) shouldBe null
    }

    "writes fail loudly" {
        shouldThrowAny { storage.add(org, UserId("b2b_users/u1"), roles = setOf("owner")) }
        shouldThrowAny { storage.save(member) }
        shouldThrowAny { storage.remove(member) }
    }
})
