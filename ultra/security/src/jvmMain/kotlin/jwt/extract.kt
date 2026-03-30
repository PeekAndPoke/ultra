package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import io.peekandpoke.ultra.security.user.UserPermissions

/** Converts this claim to a [Set] of strings, or an empty set if null. */
fun Claim.asStringSet(): Set<String> = asList(String::class.java)?.toSet() ?: emptySet()

/** Extracts [JwtUserData] from this payload using claims under the given [namespace]. */
fun Payload.extractUser(namespace: String = "user"): JwtUserData = JwtUserData(
    id = getClaim("$namespace/id").asString()
        ?: subject
        ?: "",
    desc = getClaim("$namespace/desc").asString()
        ?: getClaim("user-desc").asString()
        ?: "",
    type = getClaim("$namespace/type").asString()
        ?: getClaim("user-type").asString()
        ?: "",
    email = getClaim("$namespace/email")?.asString(),
)

/** Extracts [UserPermissions] from this payload using claims under the given [namespace]. */
fun Payload.extractPermissions(namespace: String = "permissions"): UserPermissions = UserPermissions(
    isSuperUser = getClaim("$namespace/superuser")?.asBoolean() ?: false,
    organisations = getClaim("$namespace/organisations").asStringSet(),
    branches = getClaim("$namespace/branches").asStringSet(),
    groups = getClaim("$namespace/groups").asStringSet(),
    roles = getClaim("$namespace/roles").asStringSet(),
    permissions = getClaim("$namespace/permissions").asStringSet(),
)
