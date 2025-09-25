package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.addons.toasts.ToastsStage
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.routing.router
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

val routes = Routes()

val router: Router = router {
    mount(routes)

    catchAll { NotFoundPage() }
}

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    PreactVDomEngine(mountPoint) {
        AddonExamplesApp()
    }

    router.navigateToWindowUri()
}
