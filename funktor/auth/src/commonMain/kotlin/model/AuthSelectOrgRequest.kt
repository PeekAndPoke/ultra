package io.peekandpoke.funktor.auth.model

import io.peekandpoke.ultra.security.user.OrgId
import kotlinx.serialization.Serializable

/**
 * Body for `POST /auth/{realm}/select-org` — completes an org-selection sign-in (the "n orgs" case)
 * by exchanging the [selectionToken] plus the chosen [orgId] for a full session.
 */
@Serializable
data class AuthSelectOrgRequest(
    val selectionToken: String,
    /** The chosen organisation's full Vault `_id` — as handed out by `AuthOrgRef.id`. */
    val orgId: OrgId,
)
