package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.funktor.demo.server.operator.OperatorUsersRepo.Companion.asApiModel
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

/**
 * Realm for platform operators (us). Cross-tenant staff, so [orgPolicy] stays the inherited
 * [io.peekandpoke.funktor.auth.OrgPolicy.None] — no org selection at login. Operators are
 * provisioned by us (fixtures), not self-service, so only email+password SignIn is enabled.
 */
class OperatorRealm(
    deps: Lazy<AuthSystem.Deps>,
    operatorUsersRepo: Lazy<OperatorUsersRepo>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
) : AuthRealm<OperatorUser> {
    companion object {
        const val REALM = "operators"
    }

    override val deps: AuthSystem.Deps by deps
    private val operatorUsersRepo: OperatorUsersRepo by operatorUsersRepo
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val authConfig = this.deps.config.funktor.auth

    override val id: String = REALM

    override val messaging: AuthRealm.Messaging<OperatorUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@jointhebase.co",
        senderName = "Funktor Operators",
        applicationName = "Funktor Ops",
        realm = this,
    )

    override val providers by lazy {
        listOf(
            this.emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = authConfig.baseUrls["ops"]!!.trimEnd('/') + "/auth",
                ),
                capabilities = setOf(Capability.SignIn),
            ),
        )
    }

    override suspend fun loadUserById(id: String) = operatorUsersRepo.findById(id)

    override suspend fun loadUserByEmail(email: String) = operatorUsersRepo.findByEmail(email)

    override suspend fun generateJwt(user: Stored<OperatorUser>, selectedOrg: SelectedOrg?): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = user._id,
                desc = userValue.name,
                type = OperatorUserModel.USER_TYPE,
                email = userValue.email,
            ),
            permissions = UserPermissions(
                isSuperUser = userValue.isSuperUser,
            )
        ) {
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return AuthSignInResponse.Token(
            token = token,
            permissionsNs = gen.permissionsNs,
            userNs = gen.userNs,
        )
    }

    override suspend fun getUserEmail(user: Stored<OperatorUser>): String {
        return user.resolve().email
    }

    override suspend fun serializeUser(user: Stored<OperatorUser>): JsonObject {
        return Json.encodeToJsonElement(
            OperatorUserModel.serializer(), user.asApiModel()
        ).jsonObject
    }

    override suspend fun createUserForSignup(params: AuthRealm.CreateUserForSignupParams): Stored<OperatorUser> {
        error("Operator self-signup is not supported")
    }
}
