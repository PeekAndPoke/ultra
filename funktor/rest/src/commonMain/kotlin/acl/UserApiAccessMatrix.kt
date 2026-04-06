package io.peekandpoke.funktor.rest.acl

import io.peekandpoke.ultra.remote.ApiAccessLevel
import kotlinx.serialization.Serializable

/**
 * A flat access matrix describing the estimated [ApiAccessLevel] for each API endpoint,
 * evaluated for a specific user's permissions.
 *
 * Use [ApiAcl] to perform lookups against this matrix.
 */
@Serializable
data class UserApiAccessMatrix(
    val entries: List<Entry>,
) {
    @Serializable
    data class Entry(
        /** HTTP method: "GET", "POST", "PUT", "DELETE", "HEAD" */
        val method: String,
        /** URI pattern, e.g. "/funktor-conf/events/{id}" */
        val uri: String,
        /** The estimated access level for the current user */
        val level: ApiAccessLevel,
    )
}
