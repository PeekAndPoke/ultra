package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.common.plusMinutes
import io.peekandpoke.ultra.security.user.UserPermissions
import java.util.Date

/**
 * Set the expiration to [minutes] from now, measured against the **system** clock.
 *
 * [JwtGenerator.createJwt]'s own default expiry does NOT go through this — it uses the generator's
 * injected clock, so that issuance and verification agree. The two are the same in production; they
 * differ only for a generator deliberately built with another clock.
 */
fun JwtBuilder.expiresInMinutes(minutes: Long) = apply {
    withExpiresAt(Date().plusMinutes(minutes))
}

/**
 * Drops every claim under `"$namespace/"`, so the encoder that follows is AUTHORITATIVE.
 *
 * Both encoders write most claims conditionally — that is what keeps the wire shape "omit when absent",
 * which [JwtPayload] and the wire-compat fixtures depend on. But conditional writes alone only *add*:
 * anything already in the builder survives. Since `createJwt` applies the caller's builder block before
 * encoding, a caller-set `permissions/superuser` would then outlive an unprivileged `UserPermissions`.
 * Clearing first makes "cannot be overridden by the builder" true instead of merely intended.
 */
private fun JwtBuilder.clearNamespace(namespace: String) = apply {
    claims.keys.removeAll { it.startsWith("$namespace/") }
}

/** Encodes [user] data as claims under the given [namespace], replacing anything already there. */
fun JwtBuilder.encodeUser(namespace: String = "user", user: JwtUserData) = apply {
    clearNamespace(namespace)

    withClaim("$namespace/id", user.id.value)
    withClaim("$namespace/desc", user.desc)
    withClaim("$namespace/type", user.type)

    user.email?.let { withClaim("$namespace/email", it.value) }
}

/** Encodes [permissions] as claims under the given [namespace], replacing anything already there. */
fun JwtBuilder.encodePermissions(namespace: String = "permissions", permissions: UserPermissions) = apply {

    clearNamespace(namespace)

    if (permissions.isSuperUser) {
        withClaim("$namespace/superuser", permissions.isSuperUser)
    }

    permissions.org?.let {
        withClaim("$namespace/org", it.value)
    }

    if (permissions.accessibleOrgs.isNotEmpty()) {
        withArrayClaim("$namespace/accessibleOrgs", permissions.accessibleOrgs.map { it.value }.toTypedArray())
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
