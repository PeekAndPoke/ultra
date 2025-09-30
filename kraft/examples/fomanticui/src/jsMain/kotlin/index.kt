package de.peekandpoke.kraft.examples.fomanticui

import de.peekandpoke.kraft.examples.fomanticui.pages.NotFoundPage
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.semanticui.toasts.ToastsStage
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/** Create the routes */
val routes = Routes()

/** Initializes KRAFT */
val kraft = kraftApp {
    semanticUI(
        toasts = {
            stageOptions = ToastsStage.Options(
                positioning = { top.right }
            )
        }
    )

    routing {
        mount(routes)

        catchAll { NotFoundPage() }
    }
}

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    kraft.mount(element = mountPoint, engine = PreactVDomEngine()) {
        App()
    }
}
