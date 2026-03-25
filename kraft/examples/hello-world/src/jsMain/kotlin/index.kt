package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

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
