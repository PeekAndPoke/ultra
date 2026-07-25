package io.peekandpoke.funktor.saas.domain

import io.peekandpoke.funktor.saas.isolation.OrgAware
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.addons.SoftDelete
import io.peekandpoke.ultra.vault.addons.SoftDeletable
import io.peekandpoke.ultra.vault.hooks.Timestamped

/**
 * A user's membership in an organisation — the first-class org↔user link. There is at most one
 * [OrgMember] per (org, [userId]); that pair is the unique key.
 *
 * It carries the per-org [roles] (including the structural `OrgRole.OWNER`/`ADMIN`) and optional
 * [branchIds]. The realm's `getMemberships()` hook maps these rows into the session's
 * `ultra.security.OrgMembership` value objects at login, so the JWT still carries the selected org's
 * roles exactly as before — only the SOURCE of truth moved from the user record into this collection.
 *
 * [OrgAware]: a membership belongs to exactly one organisation, so member-management routes
 * (`/orgs/{org}/members/{member}`) inherit org-isolation for free — `OrgIsolationGuard` checks that
 * the resolved member's [org] matches the request's `{org}`.
 *
 * [SoftDeletable]: removing a member soft-deletes the row (retains an audit trail of who was a
 * member and when they were removed; recoverable). `OrgMembersStorage` reads exclude soft-deleted
 * rows explicitly (via the `notDeleted` filter), so a removed member vanishes from lists, the JWT,
 * and the last-owner count.
 */
@Vault
data class OrgMember(
    override val org: Ref<Organisation>,
    /** The realm-qualified user `_id` (globally unique across the per-realm user stores). */
    val userId: UserId,
    val roles: Set<String> = emptySet(),
    val branchIds: Set<String> = emptySet(),
    override val softDelete: SoftDelete? = null,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped, OrgAware, SoftDeletable.Mutable<OrgMember> {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    override fun withSoftDelete(softDelete: SoftDelete?) = copy(softDelete = softDelete)
}
