package de.peekandpoke.ultra.security.user

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
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

                listOf(
                    tuple(
                        UserPermissions(),
                        "some-org",
                        false
                    ),
                    tuple(
                        UserPermissions(isSuperUser = true),
                        "some-org",
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "c",
                        false
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "a",
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        "b",
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = true, organisations = setOf("a", "b")),
                        "c",
                        true
                    ),
                ).forEach { (subject, test, expected) ->
                    withClue("$subject $test expects $expected") {
                        subject.hasOrganisation(test) shouldBe expected
                    }
                }
            }

            "hasAnyOrganisation" {

                listOf(
                    tuple(
                        UserPermissions(),
                        emptyList(),
                        false
                    ),
                    tuple(
                        UserPermissions(isSuperUser = true),
                        emptyList(),
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = true),
                        setOf("some-org"),
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        listOf("c"),
                        false
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        emptyList(),
                        false
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        setOf("a", "X"),
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = false, organisations = setOf("a", "b")),
                        setOf("b", "X"),
                        true
                    ),
                    tuple(
                        UserPermissions(isSuperUser = true, organisations = setOf("a", "b")),
                        setOf("Y", "X"),
                        true
                    ),
                ).forEach { (subject, test, expected) ->
                    withClue("$subject $test expects $expected") {
                        subject.hasAnyOrganisation(test) shouldBe expected
                    }
                }
            }
        }
    }
}
