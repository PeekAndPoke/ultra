package io.peekandpoke.kraft.examples.jsaddons

import io.peekandpoke.kraft.addons.avatars.avatars
import io.peekandpoke.kraft.addons.browserdetect.browserDetect
import io.peekandpoke.kraft.addons.chartjs.chartJs
import io.peekandpoke.kraft.addons.jwtdecode.jwtDecode
import io.peekandpoke.kraft.addons.marked.marked
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.addons.signaturepad.signaturePad
import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.semanticui.semanticUI
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine

val routes = Routes()

val kraft = kraftApp {
    semanticUI()

    addons {
        avatars()
        browserDetect()
        chartJs()
        jwtDecode()
        marked()
        signaturePad()
    }

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
