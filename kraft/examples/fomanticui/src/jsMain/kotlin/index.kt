package de.peekandpoke.kraft.examples.fomanticui

import de.peekandpoke.kraft.examples.fomanticui.pages.NotFoundPage
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine

/** Create the routes */
val routes = Routes()

/** Initializes KRAFT */
val kraft = kraftApp {
    semanticUI()

    routing {
        mount(routes)

        catchAll { NotFoundPage() }
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        App()
    }
}
