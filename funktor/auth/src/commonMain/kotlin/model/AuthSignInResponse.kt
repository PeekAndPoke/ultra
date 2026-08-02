package io.peekandpoke.funktor.auth.model

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
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
 * failure is an `AuthError`, which never produces one of these.
 */
@Serializable
sealed interface AuthSignInResponse {

    @Serializable
    @SerialName("success")
    data class Success(
        /** How the session is carried — see [Session]. */
        val session: Session,
        /**
         * The caller's permissions.
         *
         * Sent explicitly so **no client has to decode the token**. Before this, the browser read them
         * out of the JWT's `permissionsNs` claims, which is impossible once the token is an httpOnly
         * cookie — and was never good, because a decoded claim comes from a blob the user can rewrite.
         *
         * Still DISPLAY-ONLY. The server re-derives permissions from the verified token on every
         * request; nothing here is an authorization decision.
         */
        val permissions: UserPermissions,
        /**
         * When the session expires — drives the client's refresh scheduling.
         *
         * Nullable because `exp` is optional: RFC 7519 makes it so, the verifier treats an absent one as
         * "no expiry check" (pinned by `JwtWireCompatSpec`), and each realm supplies its own claim
         * lambda. Null means the token states no expiry, which is what the client should be told.
         */
        val expiresAt: MpInstant? = null,
        /** The signed-in user's id, or null if the token carried no usable `sub`. */
        val userId: UserId? = null,
        val realm: AuthRealmModel,
        val user: JsonObject,
        /** The organisation selected for this session. Null for org-less (`OrgPolicy.None`) realms. */
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
     * It exists so a client can tell this apart from a wrong password and offer "resend the activation
     * email", which is impossible if the only signal is an error string.
     */
    @Serializable
    @SerialName("activation-required")
    data class ActivationRequired(
        val realm: AuthRealmModel,
        /**
         * Short-lived, single-use proof that the credential check passed — the authorization for
         * `resend-activation`, exactly as [OrgSelectionRequired.selectionToken] authorizes `selectOrg`.
         *
         * Grants nothing else: it cannot sign in, and it can only ever cause a mail to the address
         * already on the account.
         */
        val resendToken: String,
    ) : AuthSignInResponse

    /**
     * How the session is carried back to the client.
     *
     * **Only [Bearer] exists.** Sealed with a single variant on purpose: it keeps the discriminator in
     * the wire format and in the generated TypeScript, so adding a transport later is additive rather
     * than a breaking reshape of every client.
     *
     * An `httpOnly` cookie variant was designed and **dropped** (maintainer, 2026-08-02). The reasoning
     * is in `.claude/tasks/20260719-token-storage-hardening.md`; the short version is that b2b2c
     * frontends run on customer-controlled custom domains, which are a different *site*, so the cookie
     * would need `SameSite=None` — losing the strongest protection precisely where it was wanted.
     */
    @Serializable
    sealed interface Session {

        /** The token travels in the response body; the client attaches it as `Authorization: Bearer`. */
        @Serializable
        @SerialName("bearer")
        data class Bearer(val token: String) : Session
    }
}

/**
 * The bearer token when the session carries one, else null — cookie mode has no token in the body.
 *
 * An extension rather than a member so it stays out of the serialized shape and out of the generated
 * TypeScript: the whole point of [AuthSignInResponse.Session] being sealed is that a client narrows
 * before reaching a token, and a nullable convenience field on the wire would undo that.
 */
val AuthSignInResponse.Success.bearerToken: String?
    get() = (session as? AuthSignInResponse.Session.Bearer)?.token
