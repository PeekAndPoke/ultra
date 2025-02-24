package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import de.peekandpoke.ultra.security.user.UserPermissions

fun Claim.asStringSet(): Set<String> = asList(String::class.java)?.toSet() ?: emptySet()

fun Payload.extractUser(namespace: String = "user"): JwtUserData = JwtUserData(
    id = getClaim("$namespace/id").asString() ?: "",
    desc = getClaim("$namespace/desc").asString() ?: "",
    type = getClaim("$namespace/type").asString() ?: "",
    email = getClaim("$namespace/email")?.asString(),
)

fun Payload.extractPermissions(namespace: String = "permissions"): UserPermissions = UserPermissions(
    isSuperUser = getClaim("$namespace/superuser")?.asBoolean() ?: false,
    organisations = getClaim("$namespace/organisations").asStringSet(),
    branches = getClaim("$namespace/branches").asStringSet(),
    groups = getClaim("$namespace/groups").asStringSet(),
    roles = getClaim("$namespace/roles").asStringSet(),
    permissions = getClaim("$namespace/permissions").asStringSet(),
)
