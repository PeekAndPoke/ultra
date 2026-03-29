package io.peekandpoke.funktor.demo.adminapp

import io.peekandpoke.funktor.auth.authState
import io.peekandpoke.funktor.auth.pages.AuthFrontend
import io.peekandpoke.funktor.auth.pages.AuthFrontendConfig
import io.peekandpoke.funktor.cluster.FunktorClusterApiClient
import io.peekandpoke.funktor.demo.adminapp.state.AdminAppState
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.funktor.inspect.FunktorInspectUi
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiClient
import io.peekandpoke.funktor.logging.api.LoggingApiClient
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.window

val win = window.asDynamic()

// Initialize config //////////////////////////////////////////////////////////

val Config = AdminAppConfig().let {
    when (val tweak = win["tweakConfig"]) {
        null -> it
        else -> tweak(it) as AdminAppConfig
    }
}.apply {
    console.log("Config", this)
}

val Apis: AdminAppApis = AdminAppApis(Config) { State.auth().token?.token }

val State: AdminAppState = AdminAppState(
    auth = authState<AdminUserModel>(
        frontend = AuthFrontend.default(
            config = AuthFrontendConfig(
                redirectAfterLogin = Nav.dashboard(),
                backgroundImageUrl = "https://miro.medium.com/v2/resize:fit:2048/format:webp/0*m_7JMnJZnFN2338H.png",
            )
        ),
        api = Apis.auth,
        router = { kraft.router },
    ),
)

val funktorInspect = FunktorInspectUi(
    loggingApi = LoggingApiClient(Apis.config),
    clusterApi = FunktorClusterApiClient(Apis.config),
    introspectionApi = IntrospectionApiClient(Apis.config),
)

val kraft = kraftApp {
    semanticUI()

    routing {
        usePathStrategy()
        // Mount app routes
        mountNav(authState = State.auth)
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        AdminAppComponent()
    }
}
