package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UserPermissionsSpec : FreeSpec() {

    init {

        "Merging permissions" - {

            "must work for isSuperUser=false and isSuperUser=false" {

                val result = UserPermissions(isSuperUser = false) mergedWith UserPermissions(isSuperUser = false)

                result.isSuperUser shouldBe false
            }

            "must work for isSuperUser=true and isSuperUser=false" {

                val result = UserPermissions(isSuperUser = true) mergedWith UserPermissions(isSuperUser = false)

                result.isSuperUser shouldBe true
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
    }
}
