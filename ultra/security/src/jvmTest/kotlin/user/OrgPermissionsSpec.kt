package io.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class OrgPermissionsSpec : FreeSpec() {

    init {

        "buildOrgPermissions" - {

            val memberships = setOf(
                OrgMembership(orgId = "o1", branchIds = setOf("b1"), roles = setOf("admin")),
                OrgMembership(orgId = "o2", branchIds = setOf("b2", "b3"), roles = setOf("viewer")),
            )

            "with no selected org (org-less session) exposes only the accessible orgs" {
                val result = buildOrgPermissions(memberships, selected = null)

                result shouldBe UserPermissions(
                    org = null,
                    accessibleOrgs = setOf("o1", "o2"),
                    branches = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet(),
                )
            }

            "with a selected org exposes that org's branches/roles + plan permissions" {
                val selected = SelectedOrg(
                    orgId = "o2",
                    membership = memberships.first { it.orgId == "o2" },
                    planPermissions = setOf("feature.reports", "feature.export"),
                )

                val result = buildOrgPermissions(memberships, selected)

                result shouldBe UserPermissions(
                    org = "o2",
                    accessibleOrgs = setOf("o1", "o2"),
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
