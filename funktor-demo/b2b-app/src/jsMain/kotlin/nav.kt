package io.peekandpoke.funktor.demo.b2bapp

import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.b2bapp.layout.LoggedInLayout
import io.peekandpoke.funktor.demo.b2bapp.pages.DashboardPage
import io.peekandpoke.funktor.demo.b2bapp.pages.NotFoundPage
import io.peekandpoke.funktor.demo.common.B2bUserModel
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Static

object Nav {
    val auth = AuthFrontendRoutes()

    val dashboard = Static("")
    val dashboardSlash = Static("/")
}

fun RootRouterBuilder.mountNav(authState: AuthState<B2bUserModel>) {
    // Auth uris are not protected
    authState.mount(this)

    val authMiddleware = authState.routerMiddleWare(Nav.auth.login())

    // Protected uris, only available for logged in b2b users
    middleware(authMiddleware) {
        layout({ LoggedInLayout(it) }) {
            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.dashboardSlash) { DashboardPage() }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
