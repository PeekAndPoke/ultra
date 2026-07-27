package io.peekandpoke.funktor.auth

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
}
