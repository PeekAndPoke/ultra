package io.peekandpoke.funktor.demo.opsapp

import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.funktor.demo.opsapp.layout.LoggedInLayout
import io.peekandpoke.funktor.demo.opsapp.pages.DashboardPage
import io.peekandpoke.funktor.demo.opsapp.pages.NotFoundPage
import io.peekandpoke.funktor.demo.opsapp.pages.OrgEditPage
import io.peekandpoke.funktor.demo.opsapp.pages.OrgsListPage
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.Static

object Nav {
    val auth = AuthFrontendRoutes()

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val orgs = Static("/orgs")
    val orgEdit = Route1("/orgs/{id}")
}

fun RootRouterBuilder.mountNav(authState: AuthState<OperatorUserModel>) {
    // Auth uris are not protected
    authState.mount(this)

    val authMiddleware = authState.routerMiddleWare(Nav.auth.login())

    // Protected uris, only available for logged in operators
    middleware(authMiddleware) {
        layout({ LoggedInLayout(it) }) {
            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.dashboardSlash) { DashboardPage() }
            mount(Nav.orgs) { OrgsListPage() }
            mount(Nav.orgEdit) { OrgEditPage(it["id"]) }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
