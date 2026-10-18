package io.peekandpoke.funktor.demo.opsapp

import io.peekandpoke.funktor.auth.AuthSessionConfig
import io.peekandpoke.funktor.auth.authState
import io.peekandpoke.funktor.auth.pages.AuthFrontend
import io.peekandpoke.funktor.auth.pages.AuthFrontendConfig
import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.funktor.demo.opsapp.state.OpsAppState
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val Config = OpsAppConfig()

val Apis: OpsAppApis = OpsAppApis(Config) { State.auth().bearerToken }

val State: OpsAppState = OpsAppState(
    auth = authState<OperatorUserModel>(
        frontend = AuthFrontend.default(
            config = AuthFrontendConfig(
                redirectAfterLogin = Nav.dashboard(),
                title = "Funktor Ops",
            ),
            // Share the one routes instance so mount + session-expiry redirect can't drift.
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
        OpsAppComponent()
    }
}
