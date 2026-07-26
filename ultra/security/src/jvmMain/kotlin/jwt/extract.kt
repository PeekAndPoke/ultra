package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

/** Converts this claim to a [Set] of strings, or an empty set if null. */
fun Claim.asStringSet(): Set<String> = asList(String::class.java)?.toSet() ?: emptySet()

/**
 * Extracts [JwtUserData] from this payload using claims under the given [namespace].
 *
 * A token whose id claim (and `sub`) is missing, blank, or not a structurally valid [UserId] yields
 * [UserRecord.ANONYMOUS_ID] rather than throwing — extraction runs on attacker-supplied input, so it
 * must degrade to "no identity" instead of turning a malformed token into a 500. Note this is
 * strictly safer than the previous behaviour, which produced a nameless yet *authenticated* user.
 */
fun Payload.extractUser(namespace: String = "user"): JwtUserData = JwtUserData(
    id = listOfNotNull(getClaim("$namespace/id").asString(), subject)
        .firstNotNullOfOrNull { UserId.parseOrNull(it) }
        ?: UserRecord.ANONYMOUS_ID,
    desc = getClaim("$namespace/desc").asString()
        ?: getClaim("user-desc").asString()
        ?: "",
    type = getClaim("$namespace/type").asString()
        ?: getClaim("user-type").asString()
        ?: "",
    // Parsed defensively: a malformed email claim degrades to "no email" rather than
    // throwing and turning an attacker-supplied token into a 500.
    email = EmailAddress.parseOrNull(getClaim("$namespace/email")?.asString()),
)

/** Extracts [UserPermissions] from this payload using claims under the given [namespace]. */
fun Payload.extractPermissions(namespace: String = "permissions"): UserPermissions = UserPermissions(
    isSuperUser = getClaim("$namespace/superuser")?.asBoolean() ?: false,
    // Parsed defensively: a malformed claim degrades to "no selected org" / drops that entry,
    // rather than throwing and turning an attacker-supplied token into a 500.
    org = OrgId.parseOrNull(getClaim("$namespace/org").asString()),
    accessibleOrgs = getClaim("$namespace/accessibleOrgs").asStringSet().mapNotNull { OrgId.parseOrNull(it) }.toSet(),
    branches = getClaim("$namespace/branches").asStringSet(),
    groups = getClaim("$namespace/groups").asStringSet(),
    roles = getClaim("$namespace/roles").asStringSet(),
    permissions = getClaim("$namespace/permissions").asStringSet(),
)
