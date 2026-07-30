package io.peekandpoke.funktor.saas

import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.UserId

/**
 * Maps the user's stored `OrgMember` rows into the session [OrgMembership] value objects the auth
 * layer feeds into the JWT — the storage side of a realm's `AuthRealm.getMemberships()` hook.
 *
 * `orgId` is the organisation's full `collection/key` `_id` (the [OrgId] contract), read straight off
 * the member's `org` ref without resolving it.
 */
suspend fun OrgMembersStorage.sessionMembershipsOf(userId: UserId): Set<OrgMembership> =
    findByUser(userId).map { stored ->
        val member = stored.value()
        OrgMembership(
            orgId = OrgId(member.org._id),
            branchIds = member.branchIds,
            roles = member.roles,
        )
    }.toSet()
