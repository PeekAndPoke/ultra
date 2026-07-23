package io.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class OrgRoleSpec : FreeSpec() {

    private fun membership(vararg roles: String) = OrgMembership(orgId = "o1", roles = roles.toSet())

    init {

        "OrgRole.structural is exactly owner + admin" {
            OrgRole.structural shouldBe setOf("owner", "admin")
        }

        "membership role predicates" - {

            "isOwner is true only when the OWNER role is present" {
                membership(OrgRole.OWNER).isOwner shouldBe true
                membership(OrgRole.ADMIN).isOwner shouldBe false
                membership("accountant").isOwner shouldBe false
                membership().isOwner shouldBe false
            }

            "isAdmin is true only when the ADMIN role is present" {
                membership(OrgRole.ADMIN).isAdmin shouldBe true
                membership(OrgRole.OWNER).isAdmin shouldBe false
                membership().isAdmin shouldBe false
            }

            "canManageMembers holds for owners and admins, not plain members" {
                membership(OrgRole.OWNER).canManageMembers shouldBe true
                membership(OrgRole.ADMIN).canManageMembers shouldBe true
                membership(OrgRole.OWNER, "accountant").canManageMembers shouldBe true
                membership("member").canManageMembers shouldBe false
                membership().canManageMembers shouldBe false
            }
        }

        "role-set predicates are the shared core (reused by the stored OrgMember row)" {
            setOf(OrgRole.OWNER).isOrgOwner shouldBe true
            setOf(OrgRole.ADMIN).isOrgOwner shouldBe false
            setOf(OrgRole.ADMIN).isOrgAdmin shouldBe true
            setOf(OrgRole.OWNER).isOrgAdmin shouldBe false
            setOf(OrgRole.OWNER).canManageOrgMembers shouldBe true
            setOf(OrgRole.ADMIN).canManageOrgMembers shouldBe true
            setOf("member").canManageOrgMembers shouldBe false
            emptySet<String>().canManageOrgMembers shouldBe false
        }

        "ownerIdsOf selects only the members holding OWNER" {
            val members = mapOf(
                "u1" to membership(OrgRole.OWNER),
                "u2" to membership(OrgRole.ADMIN),
                "u3" to membership(OrgRole.OWNER, "accountant"),
                "u4" to membership("member"),
            )

            ownerIdsOf(members) shouldBe setOf("u1", "u3")
        }

        "wouldRemoveLastOwner" - {

            "is true when the member is the sole owner" {
                wouldRemoveLastOwner(currentOwnerIds = setOf("u1"), memberId = "u1") shouldBe true
            }

            "is false when other owners remain" {
                wouldRemoveLastOwner(currentOwnerIds = setOf("u1", "u2"), memberId = "u1") shouldBe false
            }

            "is false when the member is not an owner" {
                wouldRemoveLastOwner(currentOwnerIds = setOf("u1"), memberId = "u2") shouldBe false
            }

            "is false for an org that already has no owner" {
                wouldRemoveLastOwner(currentOwnerIds = emptySet(), memberId = "u1") shouldBe false
            }
        }
    }
}
