package de.peekandpoke.kraft.examples.fomanticui

import de.peekandpoke.kraft.examples.fomanticui.pages.NotFoundPage
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.routing.router
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.semanticui.toasts.ToastsStage
import de.peekandpoke.kraft.utils.ResponsiveController
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/** Initializes KRAFT */
val kraft = kraftApp {
    semanticUI(
        toasts = {
            stageOptions = ToastsStage.Options(
                positioning = { top.right }
            )
        }
    )
}

/** Create the routes */
val routes = Routes()

/** Create the router */
val router: Router = router {
    mount(routes)

    catchAll { NotFoundPage() }
}

/** Create the responsive controller */
val responsiveCtrl = ResponsiveController()

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    kraft.mount(element = mountPoint, engine = PreactVDomEngine()) {
        App()
    }

    router.navigateToWindowUri()
}
