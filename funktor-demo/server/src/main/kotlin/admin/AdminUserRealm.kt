package io.peekandpoke.funktor.demo.server.admin

import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.funktor.demo.server.admin.AdminUsersRepo.Companion.asApiModel
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
        const val REALM = "admin-user"
    }

    override val deps: AuthSystem.Deps by deps
    private val appUserRepo: AdminUsersRepo by appUserRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val googleSso: GoogleSsoAuth.Factory by googleSso
    private val githubSso: GithubSsoAuth.Factory by githubSso

    private val authConfig = this.deps.config.funktor.auth

    override val id: String = REALM

    override val messaging: AuthRealm.Messaging<AdminUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@jointhebase.co",
        senderName = "Treore Xnefgra",
        applicationName = "Funktor Demo Admin",
        realm = this,
    )

    override val providers = listOfNotNull(
        // Email / Password
        this.emailAndPassword(
            frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                recoverPasswordUrlPattern = authConfig.baseUrls["admin"] + "/recover-password/{realm}/{token}"
            ),
            capabilities = setOf(
                AuthProviderModel.Capability.SignIn,
            )
        ),
        // Google SSO
        this.deps.config.getKeyOrNull("GOOGLE_SSO_CLIENT_ID")?.let { clientId ->
            googleSso(
                googleClientId = clientId,
                capabilities = setOf(
                    AuthProviderModel.Capability.SignIn,
                )
            )
        },
        // Github SSO
        this.deps.config.getKeyOrNull("GITHUB_SSO_CLIENT_ID")?.let { clientId ->
            this.deps.config.getKeyOrNull("GITHUB_SSO_CLIENT_SECRET")?.let { secret ->
                githubSso(
                    githubClientId = clientId,
                    githubClientSecret = secret,
                    capabilities = setOf(
                        AuthProviderModel.Capability.SignIn,
                    )
                )
            }
        },
    )

    override suspend fun loadUserById(id: String) = appUserRepo.findById(id)

    override suspend fun loadUserByEmail(email: String) = appUserRepo.findByEmail(email)

    override suspend fun generateJwt(user: Stored<AdminUser>): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val token = gen.createJwt(
            user = JwtUserData(
                id = user._id,
                desc = user.value.name,
                type = AdminUserModel.USER_TYPE,
                email = user.value.email,
            ),
            permissions = UserPermissions(
                isSuperUser = user.value.isSuperUser,
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

    override suspend fun getUserEmail(user: Stored<AdminUser>): String {
        return user.value.email
    }

    override suspend fun serializeUser(user: Stored<AdminUser>): JsonObject {
        return Json.encodeToJsonElement(
            AdminUserModel.serializer(), user.asApiModel()
        ).jsonObject
    }

    override suspend fun createUserForSignup(email: String, displayName: String?): Stored<AdminUser> {
        val name = (displayName ?: email.split("@").first()).trim()

        return appUserRepo.insert(
            AdminUser(
                name = name,
                email = email.trim(),
            )
        )
    }
}
