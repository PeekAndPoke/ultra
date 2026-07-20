package io.peekandpoke.funktor.demo.b2bapp

import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.pages.AuthLogin
import io.peekandpoke.funktor.auth.pages.ResetPasswordPage
import io.peekandpoke.funktor.demo.b2bapp.layout.LoggedInLayout
import io.peekandpoke.funktor.demo.b2bapp.layout.LoggedOutLayout
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
    // Auth routes — POC of the full-control composable path: instead of the default fullscreen
    // LoginPage, the app wraps the shared AuthLogin widget in its own dedicated LoggedOutLayout.
    mount(Nav.auth.login) {
        LoggedOutLayout {
            AuthLogin(authState)
        }
    }

    // Reset-password still uses the default page (its own chrome). A chrome-less reset widget to
    // put inside the custom layout is a follow-up (see 20260720-auth-frontend-composability).
    mount(Nav.auth.resetPassword) {
        ResetPasswordPage(
            state = authState,
            provider = it[AuthFrontendRoutes.PROVIDER_PARAM],
            token = it[AuthFrontendRoutes.TOKEN_PARAM],
        )
    }

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
