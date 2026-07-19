package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * A minimal reference to an organisation, used by the auth layer for the login org-picker and the
 * "selected org" of a successful sign-in.
 *
 * The auth framework stays free of a `saas` dependency: realms map their own org models into this
 * shape via [AuthRealm.getAccessibleOrgs].
 */
@Serializable
data class AuthOrgRef(
    val id: String,
    val slug: String,
    val name: String,
)
