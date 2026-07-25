package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.demo.common.B2bUserModel
import io.peekandpoke.funktor.demo.common.b2b.AddMemberRequest
import io.peekandpoke.funktor.demo.common.b2b.B2bMembersApiClient
import io.peekandpoke.funktor.demo.common.b2b.OrgMemberModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.isolation.OrgAwareParam
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.user.OrgRole
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.isOrgOwner
import io.peekandpoke.ultra.security.user.wouldRemoveLastOwner
import io.peekandpoke.ultra.vault.Stored
import kotlin.time.Duration.Companion.seconds

/**
 * Member management for a b2b account admin's OWN organisation.
 *
 * The org is the tenant boundary carried in the URL ([OrgAwareParam] → `OrgIsolationGuard` binds it
 * to the caller's SELECTED session org); `{member}` is a `Stored<OrgMember>`, itself `OrgAware`, so
 * the guard also checks the member belongs to `{org}`.
 *
 * Gating (floor `forUserType(B2bUser)` — b2b users only; operators use their own surface):
 * - **list**: any member of the org (the guard binds the caller's selected session org to `{org}`).
 * - **mutations**: owner/admin (`forAnyRole(OWNER, ADMIN)`).
 * - **owner-only ownership**: an op that ADDS or REMOVES ownership (grants OWNER, or targets a
 *   current owner to demote/remove) requires the CALLER to be an owner → else 403 (the standard SaaS
 *   model — admins manage members, owners manage ownership).
 * - **b2b-scoped**: mutations act only on b2b-realm members (the target `userId` must resolve in
 *   [B2bUsersRepo]) — symmetric with the list; b2b2c end-users are the operator surface's concern.
 *
 * The mutation target is re-loaded INSIDE the per-org lock (so a soft-deleted / concurrently-removed
 * member is a 404, never resurrected), and the last-owner invariant is computed there over the FULL
 * (cross-realm) active owner set.
 *
 * NOTE (revocation): both removal and demotion leave the target ~1h of residual JWT owner-power
 * (gap #4 — the accepted staleness; a stale-token holder can still act on OTHER members until their
 * JWT refreshes). The difference is SELF-reversibility: a DEMOTED owner can use that window to
 * re-promote itself, whereas a REMOVED owner cannot — its self-targeted reload 404s (`notDeleted`).
 * So to make ownership revocation stick, REMOVE rather than demote.
 */
class B2bMembersApi(
    private val services: B2bMembersServices,
) : ApiRoutes("b2b-members", authFloor = { forUserType(B2bUserModel.USER_TYPE) }) {

    data class OrgParam(override val org: Stored<Organisation>) : OrgAwareParam
    data class MemberParam(
        override val org: Stored<Organisation>,
        val member: Stored<OrgMember>,
    ) : OrgAwareParam

    private sealed interface Outcome {
        data class Ok(val member: Stored<OrgMember>) : Outcome
        data object NotFound : Outcome    // target is not an active b2b member of the org
        data object OwnerOnly : Outcome   // ownership-touching op attempted by a non-owner
        data object LastOwner : Outcome   // would leave the org with no owner
        data object AlreadyMember : Outcome // add target is already an active member of the org
    }

    val list = B2bMembersApiClient.List.mount(OrgParam::class) {
        docs {
            name = "List organisation members"
        }.codeGen {
            funcName = "list"
        }.handle { params ->
            ApiResponse.ok(loadMembers(params.org))
        }
    }

    val add = B2bMembersApiClient.Add.mount(OrgParam::class) {
        authorize {
            forAnyRole(OrgRole.OWNER, OrgRole.ADMIN)
        }.docs {
            name = "Add organisation member"
        }.codeGen {
            funcName = "add"
        }.handle { params, body ->
            val callerIsOwner = user.permissions.roles.isOrgOwner
            respond(
                withOrgLock(params.org) {
                    addMember(params.org, body, callerIsOwner)
                }
            )
        }
    }

    val changeRoles = B2bMembersApiClient.ChangeRoles.mount(MemberParam::class) {
        authorize {
            forAnyRole(OrgRole.OWNER, OrgRole.ADMIN)
        }.docs {
            name = "Change member roles"
        }.codeGen {
            funcName = "changeRoles"
        }.handle { params, body ->
            val callerIsOwner = user.permissions.roles.isOrgOwner
            respond(
                withOrgLock(params.org) {
                    resolveTarget(params)?.let { current ->
                        val touchesOwnership = body.roles.isOrgOwner || current.value().roles.isOrgOwner
                        when {
                            touchesOwnership && !callerIsOwner -> Outcome.OwnerOnly
                            // (org, userId) is immutable — only the roles change.
                            wouldOrphanOrg(params.org, current.value().userId, body.roles) -> Outcome.LastOwner
                            else -> Outcome.Ok(services.orgMembers.save(current.modify { it.copy(roles = body.roles) }))
                        }
                    } ?: Outcome.NotFound
                }
            )
        }
    }

    val remove = B2bMembersApiClient.Remove.mount(MemberParam::class) {
        authorize {
            forAnyRole(OrgRole.OWNER, OrgRole.ADMIN)
        }.docs {
            name = "Remove member"
        }.codeGen {
            funcName = "remove"
        }.handle { params ->
            val callerIsOwner = user.permissions.roles.isOrgOwner
            respond(
                withOrgLock(params.org) {
                    resolveTarget(params)?.let { current ->
                        when {
                            current.value().roles.isOrgOwner && !callerIsOwner -> Outcome.OwnerOnly
                            wouldOrphanOrg(params.org, current.value().userId, newRoles = emptySet()) -> Outcome.LastOwner
                            else -> {
                                services.orgMembers.remove(current) // soft-delete
                                Outcome.Ok(current)
                            }
                        }
                    } ?: Outcome.NotFound
                }
            )
        }
    }

    /**
     * Re-loads the mutation target INSIDE the lock (fresh, `notDeleted`-filtered) and scopes it to
     * b2b-realm members. `null` → the caller sees 404 (a soft-deleted, gone, or non-b2b target) —
     * this is what makes the write consistent with the owner-count read and closes the resurrection
     * race (a snapshot captured before a concurrent remove can no longer be written back).
     */
    private suspend fun resolveTarget(params: MemberParam): Stored<OrgMember>? {
        val current = services.orgMembers.findByOrgAndUser(params.org.asRef, params.member.value().userId)
            ?: return null
        // b2b-realm scope — symmetric with the list.
        if (b2bUserOf(current.value().userId) == null) return null
        return current
    }

    /**
     * The b2b user for [userId], or null when it is not a b2b-realm id. `findById` matches by bare
     * key, so verify the FULL id round-trips — a cross-realm bare-key collision must not pass the
     * b2b scope check (`b2b2c_users/x` must not resolve as `b2b_users/x`).
     */
    private suspend fun b2bUserOf(userId: UserId) =
        services.b2bUsers.findById(userId.value)?.takeIf { it._id == userId.value }

    /**
     * Adds an EXISTING b2b user (resolved by [AddMemberRequest.email]) to [org]. b2b-scoped: an email
     * that does not resolve to a b2b user is [Outcome.NotFound], symmetric with the rest of the
     * surface. Reactivates a soft-deleted `(org, userId)` slot rather than colliding on the unique
     * index. Called INSIDE the per-org lock so the resolve-then-write is atomic.
     *
     * NOTE (v1): reactivation is a DESTRUCTIVE overwrite — it clears the retained `SoftDelete` and the
     * prior roles/branchIds, so the "who-was-removed-and-when" record is lost, and `createdAt` is kept
     * (a re-added member's "member since" spans the removal gap). Acceptable at demo scope; an
     * append-only membership-event log is the fuller answer if audit/tenure ever matters.
     */
    private suspend fun addMember(org: Stored<Organisation>, body: AddMemberRequest, callerIsOwner: Boolean): Outcome {
        // Canonicalize to match how emails are STORED (signup lowercases via CreateUserForSignupParams)
        // — findByEmail is a case-sensitive exact match, so a raw mixed-case input would miss.
        val u = services.b2bUsers.findByEmail(body.email.trim().lowercase()) ?: return Outcome.NotFound
        // Owner-only ownership — granting OWNER on add requires the caller to be an owner.
        if (body.roles.isOrgOwner && !callerIsOwner) return Outcome.OwnerOnly

        val existing = services.orgMembers.findByOrgAndUserIncludingDeleted(org.asRef, UserId(u._id))
        return when {
            existing == null ->
                Outcome.Ok(services.orgMembers.add(org = org, userId = UserId(u._id), roles = body.roles))

            existing.value().softDelete == null ->
                Outcome.AlreadyMember

            else ->
                // Reactivate the retained slot: clear the soft-delete and apply the requested roles
                // (a re-add behaves like a fresh add — branchIds reset to empty).
                Outcome.Ok(
                    services.orgMembers.save(
                        existing.modify { it.copy(roles = body.roles, branchIds = emptySet(), softDelete = null) }
                    )
                )
        }
    }

    /** Serializes all owner-affecting mutations for one org so the check-then-act is atomic. */
    private suspend fun withOrgLock(org: Stored<Organisation>, block: suspend () -> Outcome): Outcome? =
        services.locks.tryToLock(key = "b2b-org-members-${org._key}", timeout = 5.seconds, handler = block)

    private suspend fun respond(outcome: Outcome?): ApiResponse<OrgMemberModel> = when (outcome) {
        null -> ApiResponse.conflict<OrgMemberModel>().withError("the organisation is busy — please retry")
        is Outcome.NotFound -> ApiResponse.notFound<OrgMemberModel>().withError("member not found")
        is Outcome.OwnerOnly -> ApiResponse.forbidden<OrgMemberModel>()
            .withError("only an owner can grant or revoke ownership")

        is Outcome.LastOwner -> ApiResponse.badRequest<OrgMemberModel>()
            .withError("an organisation must keep at least one owner")

        is Outcome.AlreadyMember -> ApiResponse.conflict<OrgMemberModel>()
            .withError("this user is already a member of the organisation")

        is Outcome.Ok -> ApiResponse.ok(memberModel(outcome.member))
    }

    /**
     * Would setting [userId]'s roles to [newRoles] leave the org with no owner? Computed over the
     * FULL (cross-realm) active owner set — called inside the per-org lock so it is race-free.
     */
    private suspend fun wouldOrphanOrg(org: Stored<Organisation>, userId: UserId, newRoles: Set<String>): Boolean {
        if (newRoles.isOrgOwner) return false // the target remains an owner after the change

        val ownerIds = services.orgMembers.findByOrg(org.asRef)
            .mapNotNull { m -> m.value().userId.takeIf { m.value().roles.isOrgOwner } }
            .toSet()

        return wouldRemoveLastOwner(ownerIds, userId)
    }

    private suspend fun loadMembers(org: Stored<Organisation>): List<OrgMemberModel> =
        services.orgMembers.findByOrg(org.asRef).mapNotNull { memberModelOrNull(it) }

    /** Null when the member is not a b2b-realm user (e.g. a b2b2c end-user) — scopes the b2b list. */
    private suspend fun memberModelOrNull(m: Stored<OrgMember>): OrgMemberModel? {
        val member = m.value()
        val u = b2bUserOf(member.userId)?.value() ?: return null
        return OrgMemberModel(id = m._id, userId = member.userId, name = u.name, email = u.email, roles = member.roles)
    }

    private suspend fun memberModel(m: Stored<OrgMember>): OrgMemberModel {
        val member = m.value()
        val u = b2bUserOf(member.userId)?.value()
        return OrgMemberModel(
            id = m._id,
            userId = member.userId,
            name = u?.name ?: "",
            email = u?.email ?: "",
            roles = member.roles,
        )
    }
}
