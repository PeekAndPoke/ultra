package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.demo.common.b2b.AddMemberRequest
import io.peekandpoke.funktor.demo.common.b2b.ChangeMemberRolesRequest
import io.peekandpoke.funktor.demo.common.b2b.OrgMemberModel
import io.peekandpoke.funktor.demo.server.b2b.B2bMembersApi
import io.peekandpoke.funktor.demo.server.b2b.B2bMembersApiFeature
import io.peekandpoke.funktor.demo.server.b2b.B2bUsersRepo
import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.ultra.security.user.OrgRole
import io.peekandpoke.ultra.vault.Stored

/**
 * End-to-end coverage of the b2b org member-management API (Karango backend).
 *
 * Fixtures in acme: `owner@b2b.test` (OWNER), `single@b2b.test` + `multi@b2b.test` (ADMIN), plus
 * b2b2c end-users (excluded from the b2b surface). Asserts:
 * - **Floor / isolation**: anonymous → 401; an admin lists only THEIR org's b2b members (b2b2c
 *   excluded); a foreign org → 404 (guard caller-binding).
 * - **Owner-only ownership (Fork B)**: an ADMIN cannot grant OWNER → 403; an OWNER can.
 * - **b2b scope (Fork A)**: mutating a b2b2c end-user → 404 (symmetric with the list).
 * - **Invariant + soft-delete + reload-inside-lock**: the sole owner cannot be demoted/removed; a
 *   second owner unlocks it; remove soft-deletes; mutating the now-removed member → 404 (not resurrected).
 */
class B2bMembersApiTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(B2bMembersApiFeature::class)
    private val authApi by service(AuthApiFeature::class)
    private val orgs by service(OrgsStorage::class)
    private val orgMembers by service(OrgMembersStorage::class)
    private val b2bUsers by service(B2bUsersRepo::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    private suspend fun org(slug: String): Stored<Organisation> =
        orgs.findBySlug(slug) ?: error("org '$slug' is not seeded")

    private suspend fun b2bMember(orgSlug: String, email: String): Stored<OrgMember> {
        val theOrg = org(orgSlug)
        val user = b2bUsers.findByEmail(email) ?: error("user '$email' is not seeded")
        return orgMembers.findByOrgAndUser(theOrg.asRef, user._id) ?: error("no membership for '$email' in '$orgSlug'")
    }

    /** A b2b2c end-user's membership in the org (present because both realms resolve the same orgs). */
    private suspend fun anyB2b2cMember(orgSlug: String): Stored<OrgMember> =
        orgMembers.findByOrg(org(orgSlug).asRef).first { it.value().userId.startsWith("b2b2c_users/") }

    init {
        installAllFixturesBeforeSpec()

        api.members.list { list ->
            val signInRoute = authApi.auth.signIn

            "anonymous list is unauthorized (the b2b-user floor denies)" {
                val acme = org("acme")

                apiApp {
                    anonymous {
                        list(B2bMembersApi.OrgParam(org = acme)) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "an owner/admin lists their org's b2b members (b2b2c end-users excluded)" {
                val acme = org("acme")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        list(B2bMembersApi.OrgParam(org = acme)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<List<OrgMemberModel>>()!!.map { it.email }
                                .shouldContainExactlyInAnyOrder("owner@b2b.test", "single@b2b.test", "multi@b2b.test")
                        }
                    }
                }
            }

            "an admin cannot list a foreign org's members (guard caller-binding → 404)" {
                val globex = org("globex")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        list(B2bMembersApi.OrgParam(org = globex)) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.members.add { add ->
            // Defined BEFORE the changeRoles block so it runs while `single@` is still an admin (the
            // owner test there promotes single@ and removes owner@). Targets `noorg@b2b.test`, which no
            // other test touches, so these mutations don't perturb the shared fixtures.
            val signInRoute = authApi.auth.signIn

            "anonymous add is unauthorized (the b2b-user floor denies)" {
                val acme = org("acme")

                apiApp {
                    anonymous {
                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "noorg@b2b.test", roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "adding an unknown email → 404 (b2b-scoped: only existing b2b users can be added)" {
                val acme = org("acme")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "ghost@b2b.test", roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "an ADMIN cannot grant OWNER on add (owner-only ownership → 403)" {
                val acme = org("acme")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "noorg@b2b.test", roles = setOf(OrgRole.OWNER)),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "an admin cannot add into a foreign org (guard caller-binding → 404)" {
                val globex = org("globex")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        // single@ is an admin of acme only; adding into globex must not reach the handler.
                        add(
                            B2bMembersApi.OrgParam(org = globex),
                            AddMemberRequest(email = "noorg@b2b.test", roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "an admin adds a new b2b member (200, email canonicalized), re-adding is 409, and a removed member reactivates" {
                val acme = org("acme")
                val noorgId = b2bUsers.findByEmail("noorg@b2b.test")!!._id

                // Phase 1: add a new member, then a duplicate add is rejected.
                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        // Mixed case + whitespace on input — the server canonicalizes (trim+lowercase) to
                        // match the stored (lowercased) email, so this resolves noorg@b2b.test.
                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "  NoOrg@B2B.test  ", roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<OrgMemberModel>()!!.let {
                                it.email shouldBe "noorg@b2b.test"
                                it.roles shouldBe setOf("member")
                            }
                        }

                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "noorg@b2b.test", roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.Conflict
                        }
                    }
                }

                // Soft-delete the member (simulating a prior removal) — the setup for reactivation.
                orgMembers.remove(b2bMember("acme", "noorg@b2b.test"))
                orgMembers.findByOrgAndUser(acme.asRef, noorgId) shouldBe null

                // Phase 2: re-adding reactivates the retained slot with the NEW roles (not a collision).
                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        add(
                            B2bMembersApi.OrgParam(org = acme),
                            AddMemberRequest(email = "noorg@b2b.test", roles = setOf(OrgRole.ADMIN)),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<OrgMemberModel>()!!.roles shouldBe setOf(OrgRole.ADMIN)
                        }
                    }
                }

                // Reactivated: the member resolves again (soft-delete cleared) with the new roles.
                orgMembers.findByOrgAndUser(acme.asRef, noorgId).shouldNotBeNull().value().roles shouldBe
                        setOf(OrgRole.ADMIN)

                // Cleanup: soft-delete the added member so this block leaves acme's ACTIVE set unchanged
                // ({owner@, single@, multi@}) — keeps it order-neutral vs the list test's exact assertion.
                orgMembers.remove(b2bMember("acme", "noorg@b2b.test"))
            }
        }

        api.members.changeRoles { changeRoles ->
            // Route blocks register FreeSpec containers at spec level and cannot nest — reference the
            // `remove` route directly and invoke it via the request DSL.
            val remove = api.members.remove
            val signInRoute = authApi.auth.signIn

            "an ADMIN cannot grant the OWNER role (owner-only ownership → 403)" {
                val acme = org("acme")
                val single = b2bMember("acme", "single@b2b.test") // an admin

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        // single@ is an admin promoting itself to owner → forbidden.
                        changeRoles(
                            B2bMembersApi.MemberParam(org = acme, member = single),
                            ChangeMemberRolesRequest(roles = setOf(OrgRole.OWNER)),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "an admin cannot manage a b2b2c end-user of the org (b2b scope → 404)" {
                val acme = org("acme")
                val endUser = anyB2b2cMember("acme")

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        changeRoles(
                            B2bMembersApi.MemberParam(org = acme, member = endUser),
                            ChangeMemberRolesRequest(roles = setOf("member")),
                        ) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "an owner: the sole owner is protected; a 2nd owner unlocks it; remove soft-deletes and cannot be re-mutated" {
                val acme = org("acme")
                val owner = b2bMember("acme", "owner@b2b.test")
                val single = b2bMember("acme", "single@b2b.test") // an admin, to be promoted

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("owner@b2b.test")) {
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>().token.token
                        }
                    }

                    authenticate(token) {
                        // The sole owner cannot be demoted...
                        changeRoles(
                            B2bMembersApi.MemberParam(org = acme, member = owner),
                            ChangeMemberRolesRequest(roles = setOf(OrgRole.ADMIN)),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }

                        // ...nor removed.
                        remove(B2bMembersApi.MemberParam(org = acme, member = owner)) {
                            status shouldBe HttpStatusCode.BadRequest
                        }

                        // The owner promotes an admin to a second owner.
                        changeRoles(
                            B2bMembersApi.MemberParam(org = acme, member = single),
                            ChangeMemberRolesRequest(roles = setOf(OrgRole.OWNER)),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<OrgMemberModel>()!!.roles shouldBe setOf(OrgRole.OWNER)
                        }

                        // Now the original owner can be removed (soft-delete).
                        remove(B2bMembersApi.MemberParam(org = acme, member = owner)) {
                            status shouldBe HttpStatusCode.OK
                        }

                        // The now-removed member cannot be re-mutated — the target is re-loaded inside
                        // the lock (notDeleted-filtered), so it is a 404, never resurrected.
                        changeRoles(
                            B2bMembersApi.MemberParam(org = acme, member = owner),
                            ChangeMemberRolesRequest(roles = setOf(OrgRole.ADMIN)),
                        ) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }

                // Soft-deleted: owner@'s membership no longer resolves among acme's members.
                val ownerUser = b2bUsers.findByEmail("owner@b2b.test")!!
                orgMembers.findByOrgAndUser(acme.asRef, ownerUser._id) shouldBe null
            }
        }
    }
}
