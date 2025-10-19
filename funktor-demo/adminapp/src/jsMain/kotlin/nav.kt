package de.peekandpoke.funktor.demo.adminapp

import de.peekandpoke.funktor.demo.adminapp.layout.LoggedInLayout
import de.peekandpoke.funktor.demo.adminapp.pages.DashboardPage
import de.peekandpoke.funktor.demo.adminapp.pages.LoginPage
import de.peekandpoke.funktor.demo.adminapp.pages.NotFoundPage
import de.peekandpoke.funktor.demo.adminapp.pages.ProfilePage
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

object Nav {
    val login = Static("/login")

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")
}

fun RouterBuilder.mountNav() {
    val authMiddleware = State.auth.routerMiddleWare(Nav.login())

    mount(Nav.login) { LoginPage() }

    using(authMiddleware) {
        mount(Nav.dashboard) { LoggedInLayout { DashboardPage() } }
        mount(Nav.dashboardSlash) { LoggedInLayout { DashboardPage() } }

        mount(Nav.profile) { LoggedInLayout { ProfilePage() } }
    }

    catchAll {
        NotFoundPage()
    }
}
