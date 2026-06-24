package io.peekandpoke.kraft.examples.fomanticui

import io.peekandpoke.kraft.addons.datetime.installTimezones1970to2030
import io.peekandpoke.kraft.examples.fomanticui.pages.NotFoundPage
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

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
    // This example renders dates in named time zones (e.g. Europe/Berlin), so load a dataset.
    installTimezones1970to2030()

    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        App()
    }
}
