package io.peekandpoke.funktor.auth

import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.Route2
import io.peekandpoke.kraft.routing.Static

class AuthFrontendRoutes(mountPoint: String = DEFAULT_MOUNT_POINT) {

    companion object {
        const val DEFAULT_MOUNT_POINT = "/auth"
        const val PROVIDER_PARAM = "provider"
        const val TOKEN_PARAM = "token"
    }

    val login = Static("$mountPoint/login")

    val resetPassword = Route2("$mountPoint/{$PROVIDER_PARAM}/reset-password/{$TOKEN_PARAM}")

    /** Deep-link target of the activation mail sent at sign-up. */
    val activateAccount = Route2("$mountPoint/{$PROVIDER_PARAM}/activate/{$TOKEN_PARAM}")

    /**
     * The same page WITHOUT a token — where the login page sends someone whose account is not
     * activated yet, so they can ask for a new link.
     *
     * The email is deliberately NOT a route parameter: it would land in the address bar, the browser
     * history and any `Referer` header. It is carried in memory on `AuthState.pendingActivation`.
     */
    val resendActivation = Route1("$mountPoint/{$PROVIDER_PARAM}/activate")
}
