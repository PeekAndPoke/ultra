package io.peekandpoke.funktor

import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

@Vault
@Serializable
data class TestUser(
    val name: String,
    val email: String,
    val isSuperUser: Boolean = false,
) {
    companion object {
        const val USER_TYPE = "test-user"
    }
}

class TestUsersRepo(driver: KarangoDriver) : EntityRepository<TestUser>(
    name = "funktor_all_test_users",
    storedType = kType(),
    driver = driver,
)

class TestUserRealm(
    deps: Lazy<AuthSystem.Deps>,
    usersRepo: Lazy<TestUsersRepo>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
) : AuthRealm<TestUser> {
    companion object {
        const val REALM = "admin-user"
    }

    override val deps: AuthSystem.Deps by deps
    private val usersRepo: TestUsersRepo by usersRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword

    override val id: String = REALM

    override val messaging: AuthRealm.Messaging<TestUser> = AuthRealm.DefaultMessaging(
        senderEmail = "test@example.com",
        senderName = "Test",
        applicationName = "Funktor All Test",
        realm = this,
    )

    override val providers by lazy {
        listOfNotNull(
            emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = "https://example.com/auth",
                ),
                capabilities = setOf(Capability.SignIn, Capability.SignUp),
            ),
        )
    }

    override suspend fun loadUserById(id: String): Stored<TestUser>? {
        return usersRepo.findById(id)
    }

    override suspend fun loadUserByEmail(email: String): Stored<TestUser>? {
        return usersRepo.findFirst {
            FOR(usersRepo) {
                FILTER(it.email EQ email)
                LIMIT(1)
                RETURN(it)
            }
        }
    }

    override suspend fun generateJwt(user: Stored<TestUser>): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val token = gen.createJwt(
            user = JwtUserData(
                id = user._id,
                desc = user.value.name,
                type = TestUser.USER_TYPE,
                email = user.value.email,
            ),
            permissions = UserPermissions(
                isSuperUser = user.value.isSuperUser,
            ),
        ) {
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return AuthSignInResponse.Token(
            token = token,
            permissionsNs = gen.permissionsNs,
            userNs = gen.userNs,
        )
    }

    override suspend fun getUserEmail(user: Stored<TestUser>): String {
        return user.value.email
    }

    override suspend fun serializeUser(user: Stored<TestUser>): JsonObject {
        return Json.encodeToJsonElement(
            TestUser.serializer(),
            user.value,
        ).jsonObject
    }

    override suspend fun createUserForSignup(params: AuthRealm.CreateUserForSignupParams): Stored<TestUser> {
        return usersRepo.insert(
            TestUser(
                name = params.displayName,
                email = params.email,
            )
        )
    }
}

val TestUserModule = module {
    dynamic(TestUserRealm::class)
    dynamic(TestUsersRepo::class)
}
