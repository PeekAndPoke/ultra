package io.peekandpoke.funktor.demo.server.b2b2c

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.OrgPolicy
import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.demo.common.B2b2cUserModel
import io.peekandpoke.funktor.demo.server.accessibleActiveOrgs
import io.peekandpoke.funktor.demo.server.b2b2c.B2b2cUsersRepo.Companion.asApiModel
import io.peekandpoke.funktor.demo.server.buildVettedOrgPermissions
import io.peekandpoke.funktor.demo.server.resolveActiveSelectedOrg
import io.peekandpoke.funktor.saas.sessionMembershipsOf
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

/**
 * Realm for b2b2c (end-user) tenant users — the B2B customers' own end-users. Like [OrgPolicy.Required]
 * sibling realm b2b, the generic 0/1/n flow lives in [AuthRealm.issueSignIn]; this realm only wires
 * the shared org hooks (active-only, deduped — see `saas_org_hooks.kt`) and encodes the selected
 * org into the session permissions.
 */
class B2b2cRealm(
    deps: Lazy<AuthSystem.Deps>,
    b2b2cUsersRepo: Lazy<B2b2cUsersRepo>,
    orgs: Lazy<OrgsStorage>,
    orgMembers: Lazy<OrgMembersStorage>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
) : AuthRealm<B2b2cUser> {
    companion object {
        const val REALM = "b2b2c"
    }

    override val deps: AuthSystem.Deps by deps
    private val b2b2cUsersRepo: B2b2cUsersRepo by b2b2cUsersRepo
    private val orgs: OrgsStorage by orgs
    private val orgMembers: OrgMembersStorage by orgMembers
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword
    private val authConfig = this.deps.config.funktor.auth

    override val id: String = REALM

    override val orgPolicy: OrgPolicy = OrgPolicy.Required()

    override val messaging: AuthRealm.Messaging<B2b2cUser> = AuthRealm.DefaultMessaging(
        senderEmail = "treore@jointhebase.co",
        senderName = "Funktor B2B2C",
        applicationName = "Funktor B2B2C",
        realm = this,
    )

    override val providers by lazy {
        listOf(
            this.emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(
                    baseUrl = authConfig.baseUrls["b2b2c"]!!.trimEnd('/') + "/auth",
                ),
                capabilities = setOf(Capability.SignIn),
            ),
        )
    }

    override val users = object : AuthUserAdapter<B2b2cUser> {
        // NOTE: qualified access — inside this initializer the unqualified name would resolve to the
        // constructor parameter (Lazy<...>), not the delegated property.
        private val repo get() = this@B2b2cRealm.b2b2cUsersRepo

        override suspend fun loadById(id: String) = repo.findById(id)

        override suspend fun loadByEmail(email: String) = repo.findByEmail(email)

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams): Stored<B2b2cUser> {
            // End-users are provisioned by their organisation for now. AuthError — not error() — so
            // if a capability change ever exposes sign-up, the API fails closed as a 403, not a 500.
            throw AuthError.notSupported()
        }

        override suspend fun serialize(user: Stored<B2b2cUser>): JsonObject {
            return Json.encodeToJsonElement(
                B2b2cUserModel.serializer(), user.asApiModel()
            ).jsonObject
        }
    }

    override suspend fun getAccessibleOrgs(memberships: Set<OrgMembership>): List<AuthOrgRef> {
        return orgs.accessibleActiveOrgs(memberships)
    }

    override suspend fun resolveSelectedOrg(orgId: String, memberships: Set<OrgMembership>): SelectedOrg? {
        return orgs.resolveActiveSelectedOrg(orgId, memberships)
    }

    override suspend fun getMemberships(user: Stored<B2b2cUser>): Set<OrgMembership> {
        return orgMembers.sessionMembershipsOf(user._id)
    }

    override suspend fun generateJwt(user: Stored<B2b2cUser>, selectedOrg: SelectedOrg?): AuthSignInResponse.Token {
        val gen = deps.jwtGenerator

        val userValue = user.resolve()

        val token = gen.createJwt(
            user = JwtUserData(
                id = user._id,
                desc = userValue.name,
                type = B2b2cUserModel.USER_TYPE,
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
