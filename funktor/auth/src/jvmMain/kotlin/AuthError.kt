package io.peekandpoke.funktor.auth

open class AuthError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

    /**
     * Credentials were valid, but the account has not yet proven it owns its email address.
     *
     * A TYPE rather than just a message, because `AuthRealm.signIn` turns it into
     * [AuthSignInResponse.ActivationRequired] and a caller must never have to match on message text —
     * that breaks on rewording or translation.
     */
    class AccountNotActivated(cause: Throwable? = null) : AuthError("Account not activated", cause)

    companion object {
        fun providerNotFound(provider: String, cause: Throwable? = null) =
            AuthError("Provider '$provider' not found", cause)

        fun providerDoesNotSupportAction(provider: String, action: String, cause: Throwable? = null) =
            AuthError("Provider '$provider' does not support action '$action'", cause)

        fun userNotFound(user: String, cause: Throwable? = null) =
            AuthError("User '$user' not found", cause)

        fun notSupported(cause: Throwable? = null) =
            AuthError("Not supported", cause)

        fun invalidCredentials(cause: Throwable? = null) =
            AuthError("Invalid credentials", cause)

        /** Credentials were valid, but the user has no organisation to sign into. */
        fun noOrganisationAccess(cause: Throwable? = null) =
            AuthError("No organisation access", cause)

        /**
         * See [AccountNotActivated].
         *
         * Not an enumeration leak: reaching this requires the correct password, which already tells
         * the caller the account exists.
         */
        fun accountNotActivated(cause: Throwable? = null) = AccountNotActivated(cause)

        fun invalidRequest(cause: Throwable? = null) =
            AuthError("Invalid request", cause)

        fun weakPassword(cause: Throwable? = null) =
            AuthError("Weak password", cause)
    }
}
