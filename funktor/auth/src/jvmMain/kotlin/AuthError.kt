package de.peekandpoke.funktor.auth

open class AuthError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

    companion object {
        fun notSupported(cause: Throwable? = null) = AuthError("Not supported", cause)

        fun invalidCredentials(cause: Throwable? = null) = AuthError("Invalid credentials", cause)

        fun weakPassword(cause: Throwable? = null) = AuthError("Weak password", cause)
    }
}
