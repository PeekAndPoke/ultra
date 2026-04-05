package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val kraft = kraftApp {
    semanticUI()
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        MainPage()
    }
}
