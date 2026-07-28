package io.peekandpoke.funktor.auth.model

import io.peekandpoke.ultra.security.user.OrgId
import kotlinx.serialization.Serializable

/**
 * A minimal reference to an organisation, used by the auth layer for the login org-picker and the
 * "selected org" of a successful sign-in.
 *
 * The auth framework stays free of a `saas` dependency: realms map their own org models into this
 * shape via `AuthRealm.getAccessibleOrgs` (jvmMain, so not linkable from here).
 */
@Serializable
data class AuthOrgRef(
    /** The organisation's full Vault `_id` — see [OrgId]. Use `id.key` for a URL segment. */
    val id: OrgId,
    val slug: String,
    val name: String,
)
