package io.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class OrgPermissionsSpec : FreeSpec() {

    init {

        "buildOrgPermissions" - {

            val memberships = setOf(
                OrgMembership(orgId = orgId("o1"), branchIds = setOf("b1"), roles = setOf("admin")),
                OrgMembership(orgId = orgId("o2"), branchIds = setOf("b2", "b3"), roles = setOf("viewer")),
            )

            "with no selected org (org-less session) exposes only the accessible orgs" {
                val result = buildOrgPermissions(memberships, selected = null)

                result shouldBe UserPermissions(
                    org = null,
                    accessibleOrgs = setOf(orgId("o1"), orgId("o2")),
                    branches = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet(),
                )
            }

            "with a selected org exposes that org's branches/roles + plan permissions" {
                val selected = SelectedOrg(
                    orgId = orgId("o2"),
                    membership = memberships.first { it.orgId == orgId("o2") },
                    planPermissions = setOf("feature.reports", "feature.export"),
                )

                val result = buildOrgPermissions(memberships, selected)

                result shouldBe UserPermissions(
                    org = orgId("o2"),
                    accessibleOrgs = setOf(orgId("o1"), orgId("o2")),
                    branches = setOf("b2", "b3"),
                    roles = setOf("viewer"),
                    permissions = setOf("feature.reports", "feature.export"),
                )
            }

            "with no memberships yields empty accessibleOrgs" {
                buildOrgPermissions(emptySet(), selected = null) shouldBe UserPermissions()
            }
        }
    }
}

/** Test orgs are synthetic, but still real `collection/key` ids — OrgId's init enforces that. */
private fun orgId(key: String) = OrgId("organisation/$key")
