package io.peekandpoke.ultra.security.user

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.tuple

class UserPermissionsSpec : FreeSpec() {

    init {

        "Creation" - {

            "anonymous" {
                UserPermissions.anonymous shouldBe UserPermissions(
                    isSuperUser = false,
                    org = null,
                    accessibleOrgs = emptySet(),
                    branches = emptySet(),
                    groups = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet(),
                )
            }

            "constructor default" {
                UserPermissions() shouldBe UserPermissions(
                    isSuperUser = false,
                    org = null,
                    accessibleOrgs = emptySet(),
                    branches = emptySet(),
                    groups = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet(),
                )
            }
        }

        "Merging permissions" - {

            "must work for isSuperUser=false and isSuperUser=false" {

                val result = UserPermissions(isSuperUser = false) mergedWith UserPermissions(isSuperUser = false)

                result.isSuperUser shouldBe false
            }

            "must work for isSuperUser=true and isSuperUser=false" {

                val result = UserPermissions(isSuperUser = true) mergedWith UserPermissions(isSuperUser = false)

                result.isSuperUser shouldBe false
            }

            "must work for isSuperUser=true and isSuperUser=true" {

                val result = UserPermissions(isSuperUser = true) mergedWith UserPermissions(isSuperUser = true)

                result.isSuperUser shouldBe true
            }

            "must work for isSuperUser=false and isSuperUser=true" {

                val result = UserPermissions(isSuperUser = false) mergedWith UserPermissions(isSuperUser = true)

                result.isSuperUser shouldBe true
            }

            "must work for all other rights" {

                val first = UserPermissions(
                    org = "o1",
                    accessibleOrgs = setOf("o1"),
                    branches = setOf("b1"),
                    groups = setOf("g1"),
                    roles = setOf("r1"),
                    permissions = setOf("p1"),
                )

                val second = UserPermissions(
                    org = "o2",
                    accessibleOrgs = setOf("o2"),
                    branches = setOf("b2"),
                    groups = setOf("g2"),
                    roles = setOf("r2"),
                    permissions = setOf("p2"),
                )

                val result = first mergedWith second

                result shouldBe UserPermissions(
                    org = "o2",
                    accessibleOrgs = setOf("o1", "o2"),
                    branches = setOf("b1", "b2"),
                    groups = setOf("g1", "g2"),
                    roles = setOf("r1", "r2"),
                    permissions = setOf("p1", "p2"),
                )
            }

            "the selected org is taken from other, falling back to this" {

                (UserPermissions(org = "o1") mergedWith UserPermissions(org = "o2")).org shouldBe "o2"

                // merging in org-less extras must not clear the selected org
                (UserPermissions(org = "o1") mergedWith UserPermissions(roles = setOf("extra"))).org shouldBe "o1"

                (UserPermissions(org = null) mergedWith UserPermissions(org = "o2")).org shouldBe "o2"
            }
        }

        "Checking permissions" - {

            "hasOrganisation" {

                listOf(
                    tuple(UserPermissions(), "some-org", false),
                    tuple(UserPermissions(isSuperUser = true), "some-org", true),
                    tuple(UserPermissions(org = "a"), "a", true),
                    tuple(UserPermissions(org = "a"), "b", false),
                    tuple(UserPermissions(isSuperUser = true, org = "a"), "c", true),
                ).forEach { (subject, test, expected) ->
                    withClue("$subject $test expects $expected") {
                        subject.hasOrganisation(test) shouldBe expected
                    }
                }
            }

            "hasAnyOrganisation" {

                listOf(
                    tuple(UserPermissions(), emptyList<String>(), false),
                    tuple(UserPermissions(isSuperUser = true), emptyList<String>(), true),
                    tuple(UserPermissions(isSuperUser = true), listOf("some-org"), true),
                    tuple(UserPermissions(org = "a"), listOf("c"), false),
                    tuple(UserPermissions(org = "a"), emptyList<String>(), false),
                    tuple(UserPermissions(org = "a"), listOf("a", "X"), true),
                    tuple(UserPermissions(org = "a"), listOf("b", "X"), false),
                    tuple(UserPermissions(isSuperUser = true, org = "a"), listOf("Y", "X"), true),
                ).forEach { (subject, test, expected) ->
                    withClue("$subject $test expects $expected") {
                        subject.hasAnyOrganisation(test) shouldBe expected
                    }
                }
            }

            "canAccessOrg" {

                listOf(
                    tuple(UserPermissions(), "a", false),
                    tuple(UserPermissions(isSuperUser = true), "a", true),
                    tuple(UserPermissions(accessibleOrgs = setOf("a", "b")), "a", true),
                    tuple(UserPermissions(accessibleOrgs = setOf("a", "b")), "b", true),
                    tuple(UserPermissions(accessibleOrgs = setOf("a", "b")), "c", false),
                    tuple(UserPermissions(isSuperUser = true, accessibleOrgs = setOf("a")), "c", true),
                ).forEach { (subject, test, expected) ->
                    withClue("$subject $test expects $expected") {
                        subject.canAccessOrg(test) shouldBe expected
                    }
                }
            }
        }
    }
}
