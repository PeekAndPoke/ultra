package de.peekandpoke.funktor.demo.adminapp

import de.peekandpoke.funktor.auth.AuthFrontendRoutes
import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.mountAuth
import de.peekandpoke.funktor.cluster.mountFunktorCluster
import de.peekandpoke.funktor.demo.adminapp.layout.LoggedInLayout
import de.peekandpoke.funktor.demo.adminapp.pages.DashboardPage
import de.peekandpoke.funktor.demo.adminapp.pages.NotFoundPage
import de.peekandpoke.funktor.demo.adminapp.pages.ProfilePage
import de.peekandpoke.funktor.logging.mountFunktorLogging
import de.peekandpoke.kraft.routing.RootRouterBuilder
import de.peekandpoke.kraft.routing.Static
import io.peekandpoke.funktor.demo.common.AdminUserModel

object Nav {
    val auth = AuthFrontendRoutes()

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")
}

fun RootRouterBuilder.mountNav(authState: AuthState<AdminUserModel>) {
    val authMiddleware = State.auth.routerMiddleWare(Nav.auth.login())

    mountAuth(routes = Nav.auth, state = authState)

    middleware(authMiddleware) {
        layout({ LoggedInLayout(it) }) {

            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.dashboardSlash) { DashboardPage() }
            mount(Nav.profile) { ProfilePage() }

            // Mount funktor routes
            funktorLogging { mountFunktorLogging(this) }
            funktorCluster { mountFunktorCluster(this) }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
