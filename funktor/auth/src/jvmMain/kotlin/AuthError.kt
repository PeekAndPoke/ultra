package de.peekandpoke.funktor.auth

open class AuthError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

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

        fun weakPassword(cause: Throwable? = null) =
            AuthError("Weak password", cause)
    }
}
