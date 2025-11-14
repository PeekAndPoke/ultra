package de.peekandpoke.funktor.auth

import de.peekandpoke.kraft.routing.Route2
import de.peekandpoke.kraft.routing.Static

class AuthFrontendRoutes(mountPoint: String = "/auth") {

    val login = Static("$mountPoint/login")

    val resetPassword = Route2("$mountPoint/{realm}/reset-password/{token}")

}
