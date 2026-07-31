package io.peekandpoke.funktor.auth

import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.funktor.auth.KarangoTestAppUsersRepo.Companion.asApiModel
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.AuthProvider
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.auth.provider.GithubSsoAuth
import io.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.messaging.MessagingServices
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.funktorMessaging
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.UltraSecurityConfig
import io.peekandpoke.ultra.security.jwt.JwtConfig
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.jwt.JwtSigningKey
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.ultraSecurity
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

val testAppConfig = AppConfig.of(
    // Explicitly "test": AppConfig.of() defaults the environment to "prod", which would put this
    // harness on the DELIVERING branch of funktorMessaging. Inert today only because no sender is
    // registered here — a trap for whoever first adds one.
    ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = "test")),
    funktor = FunktorConfig(
        auth = FunktorConfig.AuthConfig(
            jwt = JwtConfig(
                keys = listOf(
                    JwtSigningKey(
                        id = "test-1",
                        secret = Redacted("test-signing-key-rfc7518-requires-sixty-four-bytes-minimum!!!!!!!"),
                    )
                ),
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
        ultraSecurity(UltraSecurityConfig.testOnly)

        funktorBroker()
        funktorRest(testAppConfig) { jwt() }
        funktorMessaging(testAppConfig)

        funktorAuth { configureAuth() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}

/** Minimal [AuthUser] stub for provider unit tests built on [MinimalTestRealm]. */
data class MinimalTestUser(
    override val email: EmailAddress = EmailAddress("user@example.com"),
    val name: String = "Minimal Test User",
    override val language: LanguageSettings = LanguageSettings.default,
) : AuthUser

class TestMessaging(
    val onSendPasswordChangedEmail: suspend (Stored<MinimalTestUser>) -> EmailResult = {
        error("sendPasswordChangedEmail was not expected to be called")
    },
    val onSendPasswordRecoveryEmil: suspend (Stored<MinimalTestUser>, String) -> EmailResult = { _, _ ->
        error("sendPasswordRecoveryEmil was not expected to be called")
    },
    val onSendAccountActivationEmail: suspend (Stored<MinimalTestUser>, String) -> EmailResult = { _, _ ->
        error("sendAccountActivationEmail was not expected to be called")
    },
) : AuthRealm.Messaging<MinimalTestUser> {
    override suspend fun sendPasswordChangedEmail(user: Stored<MinimalTestUser>): EmailResult =
        onSendPasswordChangedEmail(user)

    override suspend fun sendPasswordRecoveryEmil(user: Stored<MinimalTestUser>, resetUrl: String): EmailResult =
        onSendPasswordRecoveryEmil(user, resetUrl)

    override suspend fun sendAccountActivationEmail(
        user: Stored<MinimalTestUser>,
        activationUrl: String,
    ): EmailResult = onSendAccountActivationEmail(user, activationUrl)
}

class MinimalTestDeps : AuthSystem.Deps {
    override val config: AppConfig get() = error("Not needed for test")

    override val kronos: Kronos get() = error("Not needed for test")

    override val messaging: MessagingServices get() = error("Not needed for test")

    override val jwtGenerator: JwtGenerator get() = error("Not needed for test")

    override val storage: AuthSystem.Storage get() = error("Not needed for test")

    override val passwordHasher: PasswordHasher get() = error("Not needed for test")

    override val log: Log = error("Not needed for test")

    override val random: AuthRandom get() = error("Not needed for test")
}

class MinimalTestRealm(
    override val passwordPolicy: PasswordPolicy = PasswordPolicy.default,
    /** Exposed so a spec can prove `DefaultMessaging` actually consults the realm's default. */
    override val defaultLanguage: Locale = Locale("en"),
    val getMessaging: () -> AuthRealm.Messaging<MinimalTestUser> = { TestMessaging() },
    val onLoadUserByEmail: suspend (EmailAddress) -> Stored<MinimalTestUser>? =
        { error("loadUserByEmail was not expected to be called") },
    val onLoadUserById: suspend (UserId) -> Stored<MinimalTestUser>? =
        { error("onLoadUserById was not expected to be called") },
    val onCreateUserForSignup: suspend (params: AuthUserAdapter.CreateUserForSignupParams) -> Stored<MinimalTestUser> =
        { error("createUserForSignup was not expected to be called") },
) : AuthRealm<MinimalTestUser> {
    override val id: RealmId get() = RealmId("test-realm")

    override val users = object : AuthUserAdapter<MinimalTestUser> {
        override suspend fun loadById(id: UserId): Stored<MinimalTestUser>? =
            onLoadUserById(id)

        override suspend fun loadByEmail(email: EmailAddress): Stored<MinimalTestUser>? =
            onLoadUserByEmail(email)

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<MinimalTestUser> =
            onCreateUserForSignup(params)

        override suspend fun serialize(user: Stored<MinimalTestUser>): JsonObject =
            error("Not needed for test")
    }

    override val messaging get() = getMessaging()

    override val deps get() = error("Not needed for test")

    override val providers get() = error("Not needed for test")

    override suspend fun generateJwt(user: Stored<MinimalTestUser>, selectedOrg: SelectedOrg?) =
        error("Not needed for test")
}

@Vault
data class TestAppUser(
    val name: String,
    override val email: EmailAddress,
) : AuthUser {
    companion object {
        const val USER_TYPE = "test-app-user"
    }

    override val displayName: String get() = name
}

@Serializable
data class TestAppUserModel(
    val id: String,
    val name: String,
    val email: EmailAddress,
)

class KarangoTestAppUsersRepo(driver: KarangoDriver) : EntityRepository<TestAppUser>(
    name = "test_app_users",
    storedType = kType(),
    driver = driver,
) {
    companion object {
        suspend fun Storable<TestAppUser>.asApiModel() = with(resolve()) {
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
        val REALM = RealmId("admin-user")
    }

    override val deps: AuthSystem.Deps by deps
    private val appUserRepo: KarangoTestAppUsersRepo by appUserRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val googleSso: GoogleSsoAuth.Factory by googleSso
    private val githubSso: GithubSsoAuth.Factory by githubSso

    override val id: RealmId = REALM

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

    override val users = object : AuthUserAdapter<TestAppUser> {
        // NOTE: qualified access — inside this initializer the unqualified name would resolve to the
        // constructor parameter (Lazy<...>), not the delegated property.
        private val repo get() = this@TestAppUserRealm.appUserRepo

        override suspend fun loadById(id: UserId): Stored<TestAppUser>? {
            return repo.findById(id.value)
        }

        override suspend fun loadByEmail(email: EmailAddress): Stored<TestAppUser>? {
            return repo.findFirst {
                FOR(repo) {
                    FILTER(it.email EQ email)
                    LIMIT(1)
                    RETURN(it)
                }
            }
        }

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<TestAppUser> {
            return repo.insert(
                TestAppUser(
                    name = params.displayName,
                    email = params.email,
                )
            )
        }

        override suspend fun serialize(user: Stored<TestAppUser>): JsonObject {
            return Json
                .encodeToJsonElement(TestAppUserModel.serializer(), user.asApiModel())
                .jsonObject
        }
    }

    override suspend fun generateJwt(user: Stored<TestAppUser>, selectedOrg: SelectedOrg?): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = UserId(user._id),
                desc = userValue.name,
                type = TestAppUser.USER_TYPE,
                email = userValue.email,
            ),
            permissions = UserPermissions(
                isSuperUser = false,
                roles = setOf(),
            )
        ) {
            // Expires
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return AuthSignInResponse.Token(
            token = token,
            permissionsNs = gen.permissionsNs,
            userNs = gen.userNs,
        )
    }

}
