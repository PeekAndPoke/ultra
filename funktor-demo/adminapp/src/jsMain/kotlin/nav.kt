package io.peekandpoke.funktor.demo.adminapp

import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.cluster.mountFunktorCluster
import io.peekandpoke.funktor.demo.adminapp.layout.LoggedInLayout
import io.peekandpoke.funktor.demo.adminapp.pages.DashboardPage
import io.peekandpoke.funktor.demo.adminapp.pages.NotFoundPage
import io.peekandpoke.funktor.demo.adminapp.pages.ProfilePage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.CoreFeaturesPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.RestFeaturesPage
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.funktor.logging.mountFunktorLogging
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Static

object Nav {
    val auth = AuthFrontendRoutes()

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")

    object showcase {
        val core = Static("/showcase/core")
        val rest = Static("/showcase/rest")
    }
}

fun RootRouterBuilder.mountNav(authState: AuthState<AdminUserModel>) {
    // Auth uris are not protected
    authState.mount(this)

    // Get the auth middleware
    val authMiddleware = authState.routerMiddleWare(Nav.auth.login())

    // Protected uris, only available for logged in users
    middleware(authMiddleware) {
        layout({ LoggedInLayout(it) }) {

            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.dashboardSlash) { DashboardPage() }
            mount(Nav.profile) { ProfilePage() }

            // Showcase routes
            mount(Nav.showcase.core) { CoreFeaturesPage() }
            mount(Nav.showcase.rest) { RestFeaturesPage() }

            // Mount funktor routes
            funktorLogging { mountFunktorLogging(this) }
            funktorCluster { mountFunktorCluster(this) }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
