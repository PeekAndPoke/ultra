package io.peekandpoke.funktor.demo.b2bapp

import io.peekandpoke.funktor.auth.AuthSessionConfig
import io.peekandpoke.funktor.auth.authState
import io.peekandpoke.funktor.auth.pages.AuthFrontend
import io.peekandpoke.funktor.auth.pages.AuthFrontendConfig
import io.peekandpoke.funktor.demo.b2bapp.state.B2bAppState
import io.peekandpoke.funktor.demo.common.B2bUserModel
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val Config = B2bAppConfig()

val Apis: B2bAppApis = B2bAppApis(Config) { State.auth().token?.token }

val State: B2bAppState = B2bAppState(
    auth = authState<B2bUserModel>(
        frontend = AuthFrontend.default(
            config = AuthFrontendConfig(
                redirectAfterLogin = Nav.dashboard(),
            )
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
        B2bAppComponent()
    }
}
