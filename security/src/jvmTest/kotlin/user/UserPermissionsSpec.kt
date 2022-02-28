package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe

class UserPermissionsSpec : FreeSpec() {

    init {

        "Creation" - {

            "anonymous" {
                UserPermissions.anonymous shouldBe UserPermissions(
                    isSuperUser = false,
                    organisations = emptySet(),
                    branches = emptySet(),
                    groups = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet(),
                )
            }

            "constructor default" {
                UserPermissions() shouldBe UserPermissions(
                    isSuperUser = false,
                    organisations = emptySet(),
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
                    organisations = setOf("o1"),
                    branches = setOf("b1"),
                    groups = setOf("g1"),
                    roles = setOf("r1"),
                    permissions = setOf("p1"),
                )

                val second = UserPermissions(
                    organisations = setOf("o2"),
                    branches = setOf("b2"),
                    groups = setOf("g2"),
                    roles = setOf("r2"),
                    permissions = setOf("p2"),
                )

                val result = first mergedWith second

                result shouldBe UserPermissions(
                    organisations = setOf("o1", "o2"),
                    branches = setOf("b1", "b2"),
                    groups = setOf("g1", "g2"),
                    roles = setOf("r1", "r2"),
                    permissions = setOf("p1", "p2"),
                )
            }
        }

        "Checking permissions" - {

            "hasOrganisation" {

                table(
                    headers("Subject", "Test", "Expected"),
                    row(
                        UserPermissions(),
                        "some-org",
                        false
                    ),
                    row(
                        UserPermissions(isSuperUser = true),
                        "some-org",
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "c",
                        false
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "a",
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "b",
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = true, organisations = setOf("a", "b")),
                        "c",
                        true
                    ),
                ).forAll { subject, test, expected ->
                    subject.hasOrganisation(test) shouldBe expected
                }
            }

            "hasAnyOrganisation" {

                table(
                    headers("Subject", "Test", "Expected"),
                    row(
                        UserPermissions(),
                        emptyList(),
                        false
                    ),
                    row(
                        UserPermissions(isSuperUser = true),
                        emptyList(),
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = true),
                        setOf("some-org"),
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        listOf("c"),
                        false
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        emptyList(),
                        false
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        setOf("a", "X"),
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        setOf("b", "X"),
                        true
                    ),
                    row(
                        UserPermissions(isSuperUser = true, organisations = setOf("a", "b")),
                        setOf("Y", "X"),
                        true
                    ),
                ).forAll { subject, test, expected ->
                    subject.hasAnyOrganisation(test) shouldBe expected
                }
            }
        }
    }
}
