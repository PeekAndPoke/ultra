package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Base64
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserPermissions

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

            val jwt = JwtBuilder(JWT.create())
                .encodePermissions("testns", permissions)
                .delegate
                .sign(Algorithm.none())

            val decoded = JwtPayload(
                claims = Json.parseToJsonElement(
                    String(Base64.getUrlDecoder().decode(jwt.substringAfter('.').substringBefore('.')))
                ).jsonObject
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

            val jwt = JwtBuilder(JWT.create())
                .encodePermissions("testns", permissions)
                .delegate
                .sign(Algorithm.none())

            val decoded = JwtPayload(
                claims = Json.parseToJsonElement(
                    String(Base64.getUrlDecoder().decode(jwt.substringAfter('.').substringBefore('.')))
                ).jsonObject
            )

            val extracted = decoded.extractPermissions(namespace)

            extracted shouldBe permissions
            extracted.isSuperUser shouldBe false
            extracted shouldNotBeSameInstanceAs permissions
        }
    }
}
