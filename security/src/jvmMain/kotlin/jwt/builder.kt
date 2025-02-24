package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWTCreator
import de.peekandpoke.ultra.common.plusMinutes
import de.peekandpoke.ultra.security.user.UserPermissions
import java.util.*

/**
 * Set the expiration to [minutes] from now on
 */
fun JWTCreator.Builder.expiresInMinutes(minutes: Long) = apply {
    withExpiresAt(Date().plusMinutes(minutes))
}

fun JWTCreator.Builder.encodeUser(namespace: String = "user", user: JwtUserData) = apply {
    withClaim("$namespace/id", user.id)
    withClaim("$namespace/desc", user.desc)
    withClaim("$namespace/type", user.type)

    user.email?.let { withClaim("$namespace/email", it) }
}

fun JWTCreator.Builder.encodePermissions(namespace: String = "permissions", permissions: UserPermissions) = apply {

    if (permissions.isSuperUser) {
        withClaim("$namespace/superuser", permissions.isSuperUser)
    }

    if (permissions.organisations.isNotEmpty()) {
        withArrayClaim("$namespace/organisations", permissions.organisations.toTypedArray())
    }

    if (permissions.branches.isNotEmpty()) {
        withArrayClaim("$namespace/branches", permissions.branches.toTypedArray())
    }

    if (permissions.groups.isNotEmpty()) {
        withArrayClaim("$namespace/groups", permissions.groups.toTypedArray())
    }

    if (permissions.roles.isNotEmpty()) {
        withArrayClaim("$namespace/roles", permissions.roles.toTypedArray())
    }

    if (permissions.permissions.isNotEmpty()) {
        withArrayClaim("$namespace/permissions", permissions.permissions.toTypedArray())
    }
}
