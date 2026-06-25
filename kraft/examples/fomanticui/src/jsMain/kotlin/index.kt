package io.peekandpoke.kraft.examples.fomanticui

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
    // Named time zones (e.g. Europe/Berlin) just work — Kraft installs native, zero-data
    // timezone support by default. Override in kraftApp { } with e.g. fullTimezones() if needed.
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        App()
    }
}
