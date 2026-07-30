package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSignUpResponse(
    val signIn: AuthSignInResponse? = null,
    val requiresActivation: Boolean = false,
) {
    companion object {
        val failed = AuthSignUpResponse(signIn = null, requiresActivation = false)
    }

    /**
     * Did the sign-up produce either a session or an activation requirement?
     *
     * NOT "am I signed in": an account that must be activated first legitimately gets no session, and
     * the sign-up still succeeded. Callers that want the session check [signIn] for null.
     *
     * Still reports `false` for one case that also created an account: an `OrgPolicy.Required` realm
     * whose provider does not require activation, where `issueSignIn` throws `noOrganisationAccess`
     * for a user with no org yet (`AuthRealm.signUp`). That needs a third state to express and is not
     * reachable from any realm in this repo today.
     */
    val success: Boolean = signIn != null || requiresActivation
}
