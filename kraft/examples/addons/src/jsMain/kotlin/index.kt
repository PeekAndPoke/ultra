package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val routes = Routes()

val kraft = kraftApp {
    semanticUI()

    routing {
        useHashStrategy()

        mount(routes)
        catchAll { NotFoundPage() }
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        AddonExamplesApp()
    }
}
