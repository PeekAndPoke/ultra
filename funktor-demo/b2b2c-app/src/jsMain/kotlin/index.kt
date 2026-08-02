package io.peekandpoke.funktor.demo.b2b2capp

import io.peekandpoke.funktor.auth.AuthSessionConfig
import io.peekandpoke.funktor.auth.authState
import io.peekandpoke.funktor.auth.pages.AuthFrontend
import io.peekandpoke.funktor.auth.pages.AuthFrontendConfig
import io.peekandpoke.funktor.demo.b2b2capp.state.B2b2cAppState
import io.peekandpoke.funktor.demo.common.B2b2cUserModel
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val Config = B2b2cAppConfig()

val Apis: B2b2cAppApis = B2b2cAppApis(Config) { State.auth().bearerToken }

val State: B2b2cAppState = B2b2cAppState(
    auth = authState<B2b2cUserModel>(
        frontend = AuthFrontend.default(
            config = AuthFrontendConfig(
                // b2b2c uses a dedicated LoggedOutLayout (see nav.kt), so the default-chrome
                // branding config (title/logo) is not used here.
                redirectAfterLogin = Nav.dashboard(),
            ),
            // Share the one routes instance the hand-mounted auth routes use, so the session-expiry
            // redirect (frontend.routes.login) targets the same path the app mounts the login on.
            routes = Nav.auth,
        ),
        api = Apis.auth,
        router = { kraft.router },
        sessionConfig = AuthSessionConfig(
            onTokenRefreshed = {
                console.log("[AuthState] Token refreshed")
            },
        ),
    ),
)

val kraft = kraftApp {
    semanticUI()

    routing {
        usePathStrategy()
        mountNav(authState = State.auth)
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        B2b2cAppComponent()
    }
}
