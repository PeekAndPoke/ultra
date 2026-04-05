package io.peekandpoke.kraft.examples.jsaddons

import io.peekandpoke.kraft.examples.jsaddons.avatars.AvatarsRoutes
import io.peekandpoke.kraft.examples.jsaddons.avatars.mount
import io.peekandpoke.kraft.examples.jsaddons.browserdetect.BrowserDetectExample
import io.peekandpoke.kraft.examples.jsaddons.chartjs.ChartJsExample
import io.peekandpoke.kraft.examples.jsaddons.core.CoreRoutes
import io.peekandpoke.kraft.examples.jsaddons.core.mount
import io.peekandpoke.kraft.examples.jsaddons.jwtdecode.JwtDecodeExample
import io.peekandpoke.kraft.examples.jsaddons.marked.MarkedExample
import io.peekandpoke.kraft.examples.jsaddons.pdfjs.PdfJsRoutes
import io.peekandpoke.kraft.examples.jsaddons.pdfjs.mount
import io.peekandpoke.kraft.examples.jsaddons.pixijs.BreakoutExample
import io.peekandpoke.kraft.examples.jsaddons.prismjs.PrismJsExample
import io.peekandpoke.kraft.examples.jsaddons.signaturepad.SignaturePadExample
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Static

class Routes {

    val home = Static("")
    val homeSlash = Static("/")

    val avatars = AvatarsRoutes()
    val browserDetect = Static("/example/browser-detect")
    val chartJs = Static("/example/chart-js")
    val jwtDecode = Static("/example/jwt-decode")
    val marked = Static("/example/marked")
    val pdfjs = PdfJsRoutes()
    val pixijs = Static("/example/pixi-js")
    val prismjs = Static("/example/prism-js")
    val signaturePad = Static("/example/signature-pad")

    val core = CoreRoutes()
}

fun RootRouterBuilder.mount(routes: Routes) {
    mount(routes.home) { HomePage() }
    mount(routes.homeSlash) { HomePage() }

    mount(routes.avatars)

    mount(routes.browserDetect) { BrowserDetectExample() }
    mount(routes.chartJs) { ChartJsExample() }
    mount(routes.jwtDecode) { JwtDecodeExample() }
    mount(routes.marked) { MarkedExample() }

    mount(routes.pdfjs)

    mount(routes.pixijs) { BreakoutExample() }
    mount(routes.prismjs) { PrismJsExample() }
    mount(routes.signaturePad) { SignaturePadExample() }

    mount(routes.core)
}
