package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import de.peekandpoke.ultra.security.user.UserPermissions

fun Claim.asStringSet(): Set<String> = asList(String::class.java)?.toSet() ?: emptySet()

fun Payload.extractPermissions(namespace: String = "permissions"): UserPermissions = UserPermissions(
    organisations = getClaim("$namespace/organisations").asStringSet(),
    branches = getClaim("$namespace/branches").asStringSet(),
    groups = getClaim("$namespace/groups").asStringSet(),
    roles = getClaim("$namespace/roles").asStringSet(),
    permissions = getClaim("$namespace/permissions").asStringSet(),
)
