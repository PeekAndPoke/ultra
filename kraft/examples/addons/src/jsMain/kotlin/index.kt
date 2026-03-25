package io.peekandpoke.kraft.examples.jsaddons

import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

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
