package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.KarangoTestAppUsersRepo.Companion.asApiModel
import de.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.funktor.auth.provider.AuthProvider
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.funktor.core.broker.funktorBroker
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.config.funktor.FunktorConfig
import de.peekandpoke.funktor.messaging.MessagingServices
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.funktor.messaging.funktorMessaging
import de.peekandpoke.funktor.rest.funktorRest
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.log.ultraLogging
import de.peekandpoke.ultra.security.UltraSecurityConfig
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.security.ultraSecurity
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.Vault
import de.peekandpoke.ultra.vault.VaultConfig
import de.peekandpoke.ultra.vault.ultraVault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

val testAppConfig = AppConfig.of(
    funktor = FunktorConfig(
        auth = FunktorConfig.AuthConfig(
            jwt = JwtConfig(
                signingKey = "secret",
                issuer = "issuer",
                audience = "audience",
                permissionsNs = "permissions",
                userNs = "user",
            ),
        )
    ),
)

suspend fun createAuthTestContainer(
    configureKontainer: KontainerBuilder.() -> Unit,
    configureAuth: FunktorAuthBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        instance(testAppConfig)
        instance(Kronos.systemUtc)

        ultraLogging()
        ultraVault(VaultConfig.default)
        ultraSecurity(UltraSecurityConfig.empty)

        funktorBroker()
        funktorRest(testAppConfig) { jwt() }
        funktorMessaging()

        funktorAuth { configureAuth() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}

class TestMessaging(
    val onSendPasswordChangedEmail: suspend (Stored<Any>) -> EmailResult = {
        error("sendPasswordChangedEmail was not expected to be called")
    },
    val onSendPasswordRecoveryEmil: suspend (Stored<Any>, String) -> EmailResult = { _, _ ->
        error("sendPasswordRecoveryEmil was not expected to be called")
    },
) : AuthRealm.Messaging<Any> {
    override suspend fun sendPasswordChangedEmail(user: Stored<Any>): EmailResult =
        onSendPasswordChangedEmail(user)

    override suspend fun sendPasswordRecoveryEmil(user: Stored<Any>, resetUrl: String): EmailResult =
        onSendPasswordRecoveryEmil(user, resetUrl)
}

class MinimalTestDeps : AuthSystem.Deps {
    override val config: AppConfig get() = error("Not needed for test")

    override val kronos: Kronos get() = error("Not needed for test")

    override val messaging: MessagingServices get() = error("Not needed for test")

    override val jwtGenerator: JwtGenerator get() = error("Not needed for test")

    override val storage: AuthSystem.Storage get() = error("Not needed for test")

    override val passwordHasher: PasswordHasher get() = error("Not needed for test")

    override val random: AuthRandom get() = error("Not needed for test")
}

class MinimalTestRealm(
    override val passwordPolicy: PasswordPolicy = PasswordPolicy.default,
    val getMessaging: () -> AuthRealm.Messaging<Any> = { TestMessaging() },
    val onLoadUserByEmail: suspend (String) -> Stored<Any>? =
        { error("loadUserByEmail was not expected to be called") },
    val onLoadUserById: suspend (String) -> Stored<Any>? =
        { error("onLoadUserById was not expected to be called") },
    val onCreateUserForSignup: suspend (params: AuthRealm.CreateUserForSignupParams) -> Stored<Any> =
        { error("createUserForSignup was not expected to be called") },
    val onGetUserEmail: suspend (user: Stored<Any>) -> String =
        { error("getUserEmail was not expected to be called") },
) : AuthRealm<Any> {
    override val id: String get() = "test-realm"

    override suspend fun loadUserByEmail(email: String): Stored<Any>? =
        onLoadUserByEmail(email)

    override suspend fun loadUserById(id: String): Stored<Any>? =
        onLoadUserById(id)

    override suspend fun createUserForSignup(params: AuthRealm.CreateUserForSignupParams): Stored<Any> =
        onCreateUserForSignup(params)

    override val messaging get() = getMessaging()

    override val deps get() = error("Not needed for test")

    override val providers get() = error("Not needed for test")

    override suspend fun generateJwt(user: Stored<Any>) = error("Not needed for test")

    override suspend fun getUserEmail(user: Stored<Any>) = onGetUserEmail(user)

    override suspend fun serializeUser(user: Stored<Any>) = error("Not needed for test")
}


@Vault
data class TestAppUser(
    val name: String,
    val email: String,
) {
    companion object {
        const val USER_TYPE = "test-app-user"
    }
}

@Serializable
data class TestAppUserModel(
    val id: String,
    val name: String,
    val email: String,
)

class KarangoTestAppUsersRepo(driver: KarangoDriver) : EntityRepository<TestAppUser>(
    name = "test_app_users",
    storedType = kType(),
    driver = driver,
) {
    companion object {
        fun Storable<TestAppUser>.asApiModel() = with(value) {
            TestAppUserModel(
                id = _id,
                name = name,
                email = email,
            )
        }
    }
}

class TestAppUserRealm(
    deps: Lazy<AuthSystem.Deps>,
    appUserRepo: Lazy<KarangoTestAppUsersRepo>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
    googleSso: Lazy<GoogleSsoAuth.Factory>,
    githubSso: Lazy<GithubSsoAuth.Factory>,
) : AuthRealm<TestAppUser> {
    companion object {
        const val REALM = "admin-user"
    }

    override val deps: AuthSystem.Deps by deps
    private val appUserRepo: KarangoTestAppUsersRepo by appUserRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val googleSso: GoogleSsoAuth.Factory by googleSso
    private val githubSso: GithubSsoAuth.Factory by githubSso
    private val authConfig = this.deps.config.funktor.auth

    override val id: String = REALM

    override val messaging: AuthRealm.Messaging<TestAppUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@example.com",
        senderName = "Treore Xnefgra",
        applicationName = "Funktor Auth Test",
        realm = this,
    )

    override val providers: List<AuthProvider> by lazy {
        listOfNotNull(
            // Email / Password
            this.emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = "https://example.com/auth",
                ),
                capabilities = setOf(Capability.SignIn, Capability.SignUp)
            ),
            // Google SSO
            this.googleSso.fromAppConfig(Capability.SignIn, Capability.SignUp),
            // Github SSO
            this.githubSso.fromAppConfig(Capability.SignIn, Capability.SignUp),
        )
    }

    override suspend fun loadUserById(id: String): Stored<TestAppUser>? {
        return appUserRepo.findById(id)
    }

    override suspend fun loadUserByEmail(email: String): Stored<TestAppUser>? {
        return appUserRepo.findFirst {
            FOR(appUserRepo) {
                FILTER(it.email EQ email)
                LIMIT(1)
                RETURN(it)
            }
        }
    }

    override suspend fun generateJwt(user: Stored<TestAppUser>): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val token = gen.createJwt(
            user = JwtUserData(
                id = user._id,
                desc = user.value.name,
                type = TestAppUser.USER_TYPE,
                email = user.value.email,
            ),
            permissions = UserPermissions(
                isSuperUser = false,
                organisations = setOf(),
                roles = setOf(),
            )
        ) {
            // Expires
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return AuthSignInResponse.Token(
            token = token,
            permissionsNs = gen.config.permissionsNs,
            userNs = gen.config.userNs,
        )
    }

    override suspend fun getUserEmail(user: Stored<TestAppUser>): String {
        return user.value.email
    }

    override suspend fun serializeUser(user: Stored<TestAppUser>): JsonObject {
        return Json
            .encodeToJsonElement(TestAppUserModel.serializer(), user.asApiModel())
            .jsonObject
    }

    override suspend fun createUserForSignup(params: AuthRealm.CreateUserForSignupParams): Stored<TestAppUser> {
        return appUserRepo.insert(
            TestAppUser(
                name = params.displayName,
                email = params.email,
            )
        )
    }
}
