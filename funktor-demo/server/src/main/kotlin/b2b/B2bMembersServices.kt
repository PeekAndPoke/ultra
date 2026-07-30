package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage

/** Per-request services for [B2bMembersApi] — the org-member store, the b2b user store (for member
 *  display), and the global locks provider (for atomic last-owner enforcement). */
class B2bMembersServices(
    orgMembers: Lazy<OrgMembersStorage>,
    b2bUsers: Lazy<B2bUsersRepo>,
    locks: Lazy<GlobalLocksProvider>,
) {
    val orgMembers by orgMembers
    val b2bUsers by b2bUsers
    val locks by locks
}
