package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.peekandpoke.ultra.security.user.UserPermissions
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class JwtPermissionsRoundTripSpec : FreeSpec() {

    init {

        "Encoding and decoding user permissions in a jwt must work" {

            val namespace = "testns"

            val permissions = UserPermissions(
                isSuperUser = true,
                organisations = setOf("o1, o2"),
                branches = setOf("b1, b2"),
                groups = setOf("g1, g2"),
                roles = setOf("r1, r2"),
                permissions = setOf("p1, p2"),
            )

            val jwt = JWT.create()
                .encode("testns", permissions)
                .sign(Algorithm.none())

            val decoded = JWT.decode(jwt)

            val extracted = decoded.extractPermissions(namespace)

            extracted shouldBe permissions
            extracted.isSuperUser shouldBe true
            extracted shouldNotBeSameInstanceAs permissions
        }

        "Encoding and decoding user permissions in a jwt must work #2" {

            val namespace = "testns"

            val permissions = UserPermissions(
                isSuperUser = false,
                organisations = setOf("o1, o2"),
                branches = setOf("b1, b2"),
                groups = setOf("g1, g2"),
                roles = setOf("r1, r2"),
                permissions = setOf("p1, p2"),
            )

            val jwt = JWT.create()
                .encode("testns", permissions)
                .sign(Algorithm.none())

            val decoded = JWT.decode(jwt)

            val extracted = decoded.extractPermissions(namespace)

            extracted shouldBe permissions
            extracted.isSuperUser shouldBe false
            extracted shouldNotBeSameInstanceAs permissions
        }
    }
}
