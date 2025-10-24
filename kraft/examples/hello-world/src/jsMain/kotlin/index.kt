package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.RouterComponent
import de.peekandpoke.kraft.routing.Static
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val kraft = kraftApp {
    semanticUI()

    routing {
        val main = Static("")

        mount(main) { MainPage() }

        catchAll { NotFoundPage() }
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        RouterComponent()
    }
}
