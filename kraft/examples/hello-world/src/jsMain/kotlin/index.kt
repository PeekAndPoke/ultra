package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.addons.toasts.ToastsStage
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.Static
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

val kraft = kraftApp {
    toasts {
        stageOptions = ToastsStage.Options(
            positioning = { top.right }
        )
    }
}

val router = router {
    val main = Static("")

    mount(main) { MainPage() }

    catchAll { NotFoundPage() }
}

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    PreactVDomEngine(mountPoint) {
        kraft.mount(this) {
            RouterComponent(router)
        }
    }

    router.navigateToWindowUri()
}
