package de.peekandpoke.funktor.auth

import de.peekandpoke.kraft.routing.Route2
import de.peekandpoke.kraft.routing.Static

class AuthFrontendRoutes(mountPoint: String = DEFAULT_MOUNT_POINT) {

    companion object {
        const val DEFAULT_MOUNT_POINT = "/auth"
        const val PROVIDER_PARAM = "provider"
        const val TOKEN_PARAM = "token"
    }

    val login = Static("$mountPoint/login")

    val resetPassword = Route2("$mountPoint/{$PROVIDER_PARAM}/reset-password/{$TOKEN_PARAM}")
}
