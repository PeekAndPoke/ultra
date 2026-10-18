package io.peekandpoke.ultra.security.jwt

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserPermissions
import kotlinx.serialization.json.JsonObject

class JwtPermissionsRoundTripSpec : FreeSpec() {

    init {

        "Encoding and decoding user permissions in a jwt must work" {

            val namespace = "testns"

            val permissions = UserPermissions(
                isSuperUser = true,
                org = OrgId("organisation/o1"),
                accessibleOrgs = setOf(OrgId("organisation/o1"), OrgId("organisation/o2")),
                branches = setOf("b1", "b2"),
                groups = setOf("g1", "g2"),
                roles = setOf("r1", "r2"),
                permissions = setOf("p1", "p2"),
            )

            val decoded = JwtPayload(
                claims = JsonObject(JwtBuilder().encodePermissions(namespace, permissions).claims)
            )

            val extracted = decoded.extractPermissions(namespace)

            extracted shouldBe permissions
            extracted.isSuperUser shouldBe true
            extracted shouldNotBeSameInstanceAs permissions
        }

        "Encoding and decoding user permissions in a jwt must work #2" {

            val namespace = "testns"

            val permissions = UserPermissions(
                isSuperUser = false,
                org = OrgId("organisation/o1"),
                accessibleOrgs = setOf(OrgId("organisation/o1"), OrgId("organisation/o2")),
                branches = setOf("b1", "b2"),
                groups = setOf("g1", "g2"),
                roles = setOf("r1", "r2"),
                permissions = setOf("p1", "p2"),
            )

            val decoded = JwtPayload(
                claims = JsonObject(JwtBuilder().encodePermissions(namespace, permissions).claims)
            )

            val extracted = decoded.extractPermissions(namespace)

            extracted shouldBe permissions
            extracted.isSuperUser shouldBe false
            extracted shouldNotBeSameInstanceAs permissions
        }
    }
}
