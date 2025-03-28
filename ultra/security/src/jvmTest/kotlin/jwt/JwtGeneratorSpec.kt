package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import de.peekandpoke.ultra.security.user.UserPermissions
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JwtGeneratorSpec : StringSpec() {

    private val permissionsNs = "permissions"
    private val userNs = "user"

    // Mock JwtConfig for testing
    private val mockConfig = JwtConfig(
        issuer = "testIssuer",
        audience = "testAudience",
        singingKey = "testSigningKey",
        permissionsNs = permissionsNs,
        userNs = userNs,
    )

    // JwtGenerator instance
    private val jwtGenerator = JwtGenerator(config = mockConfig)

    init {
        "Config must be public" {
            jwtGenerator.config shouldBe mockConfig
        }

        "createJwt should generate a valid token for provided user data" {
            // Arrange
            val userData = JwtUserData(
                id = "123",
                desc = "Test User",
                type = "Admin",
                email = null,
            )
            val expectedPermissions = UserPermissions()

            // Act
            val token = jwtGenerator.createJwt(user = userData, permissions = expectedPermissions)

            // Assert
            val decodedToken = jwtGenerator.verifier.verify(token)
            val extractedUser = jwtGenerator.extractUserData(decodedToken)

            assertSoftly {
                withClue("token should contain the expected issuer, audience and subject") {
                    decodedToken.subject shouldBe userData.id
                    decodedToken.issuer shouldBe "testIssuer"
                    decodedToken.audience.first() shouldBe "testAudience"
                }

                withClue("token should contain the expected permissions") {
                    extractedUser.id shouldBe userData.id
                    extractedUser.desc shouldBe userData.desc
                    extractedUser.type shouldBe userData.type
                    extractedUser.email shouldBe userData.email
                }
            }
        }

        "createJwt should include custom claims using builder" {
            // Arrange
            val userData = JwtUserData(
                id = "123",
                desc = "Test User",
                type = "Admin",
                email = "user-123@example.com"
            )

            // Act
            val token = jwtGenerator.createJwt(user = userData) {
                withClaim("custom-claim", "custom-value")
            }

            // Assert
            val decodedToken = jwtGenerator.verifier.verify(token)
            val extractedUser = jwtGenerator.extractUserData(decodedToken)

            assertSoftly {
                withClue("custom claims should be included in the token") {
                    decodedToken.getClaim("custom-claim").asString() shouldBe "custom-value"
                }

                withClue("user data should be included in the token") {
                    extractedUser.id shouldBe userData.id
                    extractedUser.desc shouldBe userData.desc
                    extractedUser.type shouldBe userData.type
                    extractedUser.email shouldBe userData.email
                }
            }
        }

        "createJwt should throw JWTVerificationException for invalid token" {
            // Arrange
            val invalidToken = JWT.create()
                .withIssuer("invalidIssuer")
                .sign(Algorithm.HMAC512("invalidKey"))

            // Act & Assert
            shouldThrow<JWTVerificationException> {
                jwtGenerator.verifier.verify(invalidToken)
            }
        }

        "createJwt should encode permissions correctly" {
            // Arrange
            val userData = JwtUserData(
                id = "456",
                desc = "Another User",
                type = "User"
            )
            val testCases = listOf(
                UserPermissions(
                    isSuperUser = true,
                    organisations = setOf("org1", "org2"),
                    branches = setOf("branch1", "branch2"),
                    groups = setOf("group1", "group2"),
                    roles = setOf("role1", "role2"),
                    permissions = setOf("read", "write")
                ),
                UserPermissions(
                    isSuperUser = false,
                    organisations = setOf("orgA", "orgB"),
                    branches = emptySet(),
                    groups = setOf("groupX"),
                    roles = setOf("roleY", "roleZ"),
                    permissions = setOf("execute")
                ),
                UserPermissions(
                    isSuperUser = false,
                    organisations = emptySet(),
                    branches = emptySet(),
                    groups = emptySet(),
                    roles = emptySet(),
                    permissions = emptySet()
                )
            )

            assertSoftly {
                testCases.forEach { permissions ->
                    // Act
                    val token = jwtGenerator.createJwt(user = userData, permissions = permissions)

                    // Assert
                    val decodedToken = jwtGenerator.verifier.verify(token)
                    val extractedPermissions = jwtGenerator.extractPermissions(decodedToken)

                    withClue("token should contain the expected permissions") {
                        extractedPermissions.isSuperUser shouldBe permissions.isSuperUser
                        extractedPermissions.organisations shouldBe permissions.organisations
                        extractedPermissions.branches shouldBe permissions.branches
                        extractedPermissions.groups shouldBe permissions.groups
                        extractedPermissions.roles shouldBe permissions.roles
                        extractedPermissions.permissions shouldBe permissions.permissions
                    }
                }
            }
        }

        "createJwt should use default permissions when not provided" {
            // Arrange
            val userData = JwtUserData(
                id = "789",
                desc = "Default Permissions User",
                type = "None"
            )

            // Act
            val token = jwtGenerator.createJwt(user = userData)

            // Assert
            val decodedToken = jwtGenerator.verifier.verify(token)
            val extractedPermissions = decodedToken.extractPermissions(permissionsNs)

            extractedPermissions.isSuperUser shouldBe false
            extractedPermissions.organisations shouldBe emptySet()
            extractedPermissions.branches shouldBe emptySet()
            extractedPermissions.groups shouldBe emptySet()
            extractedPermissions.roles shouldBe emptySet()
            extractedPermissions.permissions shouldBe emptySet()
        }
    }
}
