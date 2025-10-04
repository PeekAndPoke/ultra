package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.semanticui.toasts.ToastsStage
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

val routes = Routes()

val kraft = kraftApp {
    semanticUI(
        toasts = {
            stageOptions = ToastsStage.Options(
                positioning = { top.right }
            )
        }
    )

    routing {
        useHashStrategy()

        mount(routes)
        catchAll { NotFoundPage() }
    }
}

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    kraft.mount(mountPoint, PreactVDomEngine()) {
        AddonExamplesApp()
    }
}
