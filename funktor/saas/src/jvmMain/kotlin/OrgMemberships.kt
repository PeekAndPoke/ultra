package io.peekandpoke.funktor.saas

import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.ultra.security.user.OrgMembership

/**
 * Maps the user's stored `OrgMember` rows into the session [OrgMembership] value objects the auth
 * layer feeds into the JWT — the storage side of a realm's `AuthRealm.getMemberships()` hook.
 *
 * `orgId` is the organisation's bare `_key` (the `UserPermissions.org` / [OrgMembership.orgId]
 * contract), read from the member's `org` ref without resolving it.
 */
suspend fun OrgMembersStorage.sessionMembershipsOf(userId: String): Set<OrgMembership> =
    findByUser(userId).map { stored ->
        val member = stored.value()
        OrgMembership(
            orgId = member.org._key,
            branchIds = member.branchIds,
            roles = member.roles,
        )
    }.toSet()
