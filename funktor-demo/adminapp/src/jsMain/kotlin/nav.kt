package io.peekandpoke.funktor.demo.adminapp

import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.adminapp.layout.LoggedInLayout
import io.peekandpoke.funktor.demo.adminapp.pages.DashboardPage
import io.peekandpoke.funktor.demo.adminapp.pages.NotFoundPage
import io.peekandpoke.funktor.demo.adminapp.pages.ProfilePage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.AttendeeEditPage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.AttendeesListPage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.EventEditPage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.EventsListPage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.SpeakerEditPage
import io.peekandpoke.funktor.demo.adminapp.pages.funktorconf.SpeakersListPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.AuthShowcasePage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.ClusterDemoPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.CoreFeaturesPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.MessagingDemoPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.RestFeaturesPage
import io.peekandpoke.funktor.demo.adminapp.pages.showcase.SseDemoPage
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.funktor.inspect.mountFunktorInspect
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.Static

object Nav {
    val auth = AuthFrontendRoutes()

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")

    object funktorConf {
        val events = Static("/funktorconf/events")
        val eventEdit = Route1("/funktorconf/events/{id}")
        val speakers = Static("/funktorconf/speakers")
        val speakerEdit = Route1("/funktorconf/speakers/{id}")
        val attendees = Static("/funktorconf/attendees")
        val attendeeEdit = Route1("/funktorconf/attendees/{id}")
    }

    object showcase {
        val core = Static("/showcase/core")
        val rest = Static("/showcase/rest")
        val auth = Static("/showcase/auth")
        val cluster = Static("/showcase/cluster")
        val messaging = Static("/showcase/messaging")
        val sse = Static("/showcase/sse")
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

            // FunktorConf routes
            mount(Nav.funktorConf.events) { EventsListPage() }
            mount(Nav.funktorConf.eventEdit) { EventEditPage(it["id"]) }
            mount(Nav.funktorConf.speakers) { SpeakersListPage() }
            mount(Nav.funktorConf.speakerEdit) { SpeakerEditPage(it["id"]) }
            mount(Nav.funktorConf.attendees) { AttendeesListPage() }
            mount(Nav.funktorConf.attendeeEdit) { AttendeeEditPage(it["id"]) }

            // Showcase routes
            mount(Nav.showcase.core) { CoreFeaturesPage() }
            mount(Nav.showcase.rest) { RestFeaturesPage() }
            mount(Nav.showcase.auth) { AuthShowcasePage() }
            mount(Nav.showcase.cluster) { ClusterDemoPage() }
            mount(Nav.showcase.messaging) { MessagingDemoPage() }
            mount(Nav.showcase.sse) { SseDemoPage() }

            // Mount unified funktor inspect routes
            funktorInspect { mountFunktorInspect(this) }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
