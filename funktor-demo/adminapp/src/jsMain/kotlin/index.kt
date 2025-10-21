package de.peekandpoke.funktor.demo.adminapp

import de.peekandpoke.funktor.auth.authState
import de.peekandpoke.funktor.cluster.FunktorClusterApiClient
import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.demo.adminapp.state.AdminAppState
import de.peekandpoke.funktor.logging.LoggingUi
import de.peekandpoke.funktor.logging.api.LoggingApiClient
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.router
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.semanticui.toasts
import de.peekandpoke.kraft.semanticui.toasts.ToastsStage
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import io.peekandpoke.funktor.demo.common.AdminUserModel
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
        api = Apis.auth,
        router = { kraft.router },
    ),
)

val funktorCluster = FunktorClusterUi(
    api = FunktorClusterApiClient(Apis.config),
)

val funktorLogging = LoggingUi(
    api = LoggingApiClient(Apis.config),
)

val kraft = kraftApp {
    semanticUI {
        toasts {
            stageOptions = ToastsStage.Options(
                positioning = { top.right }
            )
        }
    }

    routing {
        usePathStrategy()
        // Mount app routes
        mountNav()
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        AdminAppComponent()
    }
}
