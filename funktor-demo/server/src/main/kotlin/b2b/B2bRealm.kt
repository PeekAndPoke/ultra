package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.OrgPolicy
import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.demo.common.B2bUserModel
import io.peekandpoke.funktor.demo.server.accessibleActiveOrgs
import io.peekandpoke.funktor.demo.server.b2b.B2bUsersRepo.Companion.asApiModel
import io.peekandpoke.funktor.demo.server.buildVettedOrgPermissions
import io.peekandpoke.funktor.demo.server.resolveActiveSelectedOrg
import io.peekandpoke.funktor.saas.sessionMembershipsOf
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

/**
 * Realm for b2b (customer-admin) tenant users. Every user belongs to one or more organisations, so
 * [orgPolicy] is [OrgPolicy.Required] — the login flow resolves the user's accessible orgs and
 * drives the 0 (no access) / 1 (auto-select) / n (selection step) behaviour generically in
 * [AuthRealm.issueSignIn]. This realm only supplies the two org hooks (over [OrgsStorage]) and
 * encodes the selected org into the session permissions.
 */
class B2bRealm(
    deps: Lazy<AuthSystem.Deps>,
    b2bUsersRepo: Lazy<B2bUsersRepo>,
    orgs: Lazy<OrgsStorage>,
    orgMembers: Lazy<OrgMembersStorage>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
) : AuthRealm<B2bUser> {
    companion object {
        val REALM = RealmId("b2b")
    }

    override val deps: AuthSystem.Deps by deps
    private val b2bUsersRepo: B2bUsersRepo by b2bUsersRepo
    private val orgs: OrgsStorage by orgs
    private val orgMembers: OrgMembersStorage by orgMembers
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val authConfig = this.deps.config.funktor.auth

    override val id: RealmId = REALM

    override val orgPolicy: OrgPolicy = OrgPolicy.Required()

    override val messaging: AuthRealm.Messaging<B2bUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@jointhebase.co",
        senderName = "Funktor B2B",
        applicationName = "Funktor B2B",
        realm = this,
    )

    override val providers by lazy {
        listOf(
            this.emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = authConfig.baseUrls["b2b"]!!.trimEnd('/') + "/auth",
                ),
                capabilities = setOf(Capability.SignIn),
            ),
        )
    }

    override val users = object : AuthUserAdapter<B2bUser> {
        // NOTE: qualified access — inside this initializer the unqualified name would resolve to the
        // constructor parameter (Lazy<...>), not the delegated property.
        private val repo get() = this@B2bRealm.b2bUsersRepo

        override suspend fun loadById(id: UserId) = repo.findById(id.value)

        override suspend fun loadByEmail(email: EmailAddress) = repo.findByEmail(email)

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<B2bUser> {
            // B2B is invite-only (users are created inside an organisation). AuthError — not
            // error() — so if a capability change ever exposes sign-up, the API fails closed as a
            // 403, not a 500.
            throw AuthError.notSupported()
        }

        override suspend fun serialize(user: Stored<B2bUser>): JsonObject {
            return Json.encodeToJsonElement(
                B2bUserModel.serializer(), user.asApiModel()
            ).jsonObject
        }
    }

    override suspend fun getAccessibleOrgs(memberships: Set<OrgMembership>): List<AuthOrgRef> {
        return orgs.accessibleActiveOrgs(memberships)
    }

    override suspend fun resolveSelectedOrg(orgId: OrgId, memberships: Set<OrgMembership>): SelectedOrg? {
        return orgs.resolveActiveSelectedOrg(orgId, memberships)
    }

    override suspend fun getMemberships(user: Stored<B2bUser>): Set<OrgMembership> {
        return orgMembers.sessionMembershipsOf(UserId(user._id))
    }

    override suspend fun generateJwt(user: Stored<B2bUser>, selectedOrg: SelectedOrg?): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = UserId(user._id),
                desc = userValue.name,
                type = B2bUserModel.USER_TYPE,
                email = userValue.email,
            ),
            permissions = orgs.buildVettedOrgPermissions(
                memberships = getMemberships(user),
                selected = selectedOrg,
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
