package io.peekandpoke.funktor.demo.server.admin

import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.auth.provider.GithubSsoAuth
import io.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.funktor.demo.server.admin.AdminUsersRepo.Companion.asApiModel
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

class AdminUserRealm(
    deps: Lazy<AuthSystem.Deps>,
    appUserRepo: Lazy<AdminUsersRepo>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
    googleSso: Lazy<GoogleSsoAuth.Factory>,
    githubSso: Lazy<GithubSsoAuth.Factory>,
) : AuthRealm<AdminUser> {
    companion object {
        val REALM = RealmId("admin-user")
    }

    override val deps: AuthSystem.Deps by deps
    private val appUserRepo: AdminUsersRepo by appUserRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val googleSso: GoogleSsoAuth.Factory by googleSso
    private val githubSso: GithubSsoAuth.Factory by githubSso
    private val authConfig = this.deps.config.funktor.auth

    override val id: RealmId = REALM

    override val messaging: AuthRealm.Messaging<AdminUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@jointhebase.co",
        senderName = "Treore Xnefgra",
        applicationName = "Funktor Demo Admin",
        realm = this,
    )

    override val providers by lazy {
        listOfNotNull(
            // Email / Password
            this.emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = authConfig.baseUrls["admin"]!!.trimEnd('/') + "/auth",
                ),
                capabilities = setOf(Capability.SignIn, Capability.SignUp)
            ),
            // Google SSO
            this.googleSso.fromAppConfig(Capability.SignIn, Capability.SignUp),
            // Github SSO
            this.githubSso.fromAppConfig(Capability.SignIn, Capability.SignUp),
        )
    }

    override val users = object : AuthUserAdapter<AdminUser> {
        // NOTE: qualified access — inside this initializer the unqualified name would resolve to the
        // constructor parameter (Lazy<...>), not the delegated property.
        private val repo get() = this@AdminUserRealm.appUserRepo

        override suspend fun loadById(id: UserId) = repo.findById(id.value)

        override suspend fun loadByEmail(email: EmailAddress) = repo.findByEmail(email)

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<AdminUser> {
            return repo.insert(
                AdminUser(
                    name = params.displayName,
                    email = params.email,
                )
            )
        }

        override suspend fun serialize(user: Stored<AdminUser>): JsonObject {
            return Json.encodeToJsonElement(
                AdminUserModel.serializer(), user.asApiModel()
            ).jsonObject
        }
    }

    override suspend fun generateJwt(user: Stored<AdminUser>, selectedOrg: SelectedOrg?): String {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = UserId(user._id),
                desc = userValue.name,
                type = AdminUserModel.USER_TYPE,
                email = userValue.email,
            ),
            permissions = UserPermissions(
                isSuperUser = userValue.isSuperUser,
                roles = setOf(),
            )
        ) {
            // Expires
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return token
    }

}
