package io.peekandpoke.funktor

import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.RealmTokenConfig
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.RealmId
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
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Vault
@Serializable
data class TestUser(
    val name: String,
    override val email: EmailAddress,
    val isSuperUser: Boolean = false,
) : AuthUser {
    companion object {
        const val USER_TYPE = "test-user"
    }

    override val displayName: String get() = name
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
        val REALM = RealmId("admin-user")

        /** Exposed so specs can wait exactly as long as the realm is configured for. */
        val TOKEN_CONFIG = RealmTokenConfig(
            activationResendCooldown = 2.seconds,
        )
    }

    override val deps: AuthSystem.Deps by deps
    private val usersRepo: TestUsersRepo by usersRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword

    override val id: RealmId = REALM

    /**
     * Only the activation-resend cooldown deviates from the defaults, and only so that BOTH sides of
     * it stay testable against a real clock: `AccountActivationEmailE2eSpec` waits this out to prove a
     * resend delivers a new link, and resends back-to-back to prove the window suppresses one. The
     * production default is minutes, which no test can wait for.
     */
    override val tokenConfig = TOKEN_CONFIG

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

    override val users = object : AuthUserAdapter<TestUser> {
        // NOTE: qualified access — inside this initializer the unqualified name would resolve to the
        // constructor parameter (Lazy<...>), not the delegated property.
        private val repo get() = this@TestUserRealm.usersRepo

        override suspend fun loadById(id: UserId): Stored<TestUser>? {
            return repo.findById(id.value)
        }

        override suspend fun loadByEmail(email: EmailAddress): Stored<TestUser>? {
            return repo.findFirst {
                FOR(repo) {
                    FILTER(it.email EQ email)
                    LIMIT(1)
                    RETURN(it)
                }
            }
        }

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<TestUser> {
            return repo.insert(
                TestUser(
                    name = params.displayName,
                    email = params.email,
                )
            )
        }

        override suspend fun serialize(user: Stored<TestUser>): JsonObject {
            return Json.encodeToJsonElement(
                TestUser.serializer(),
                user.resolve(),
            ).jsonObject
        }
    }

    override suspend fun generateJwt(user: Stored<TestUser>, selectedOrg: SelectedOrg?): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = UserId(user._id),
                desc = userValue.name,
                type = TestUser.USER_TYPE,
                email = userValue.email,
            ),
            permissions = UserPermissions(
                isSuperUser = userValue.isSuperUser,
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

}

val TestUserModule = module {
    dynamic(TestUserRealm::class)
    dynamic(TestUsersRepo::class)
}
