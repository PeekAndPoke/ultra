package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * The result of a sign-in attempt.
 *
 * - [Success] — the user is signed in; carries the JWT and (for org realms) the selected org.
 * - [OrgSelectionRequired] — credentials were valid but the user belongs to multiple organisations
 *   and must pick one; carries a short-lived selection token and the choices.
 * - [ActivationRequired] — credentials were valid but the account has not proven it owns its email
 *   address yet.
 *
 * The last two are NOT failures: the credential check passed and there is a defined next step. A
 * failure is an [AuthError], which never produces one of these.
 */
@Serializable
sealed interface AuthSignInResponse {

    @Serializable
    @SerialName("success")
    data class Success(
        val token: Token,
        val realm: AuthRealmModel,
        val user: JsonObject,
        /** The organisation selected for this session. Null for org-less ([OrgPolicy.None]) realms. */
        val org: AuthOrgRef? = null,
    ) : AuthSignInResponse {
        fun <T> getTypedUser(serializer: DeserializationStrategy<T>): T {
            return Json.decodeFromJsonElement(serializer, user)
        }
    }

    @Serializable
    @SerialName("org-selection-required")
    data class OrgSelectionRequired(
        val realm: AuthRealmModel,
        /** Short-lived, single-use token proving the credential check passed. */
        val selectionToken: String,
        /** The organisations the user may select from. */
        val organisations: List<AuthOrgRef>,
    ) : AuthSignInResponse

    /**
     * The password was right and the account exists — it just has not been activated yet.
     *
     * Carries NO token and no user data, so it grants nothing. It exists so a client can tell this
     * apart from a wrong password and offer "resend the activation email", which is impossible if the
     * only signal is an error string.
     */
    @Serializable
    @SerialName("activation-required")
    data class ActivationRequired(
        val realm: AuthRealmModel,
    ) : AuthSignInResponse

    @Serializable
    @SerialName("token")
    data class Token(
        val token: String,
        val permissionsNs: String,
        val userNs: String,
    )
}
