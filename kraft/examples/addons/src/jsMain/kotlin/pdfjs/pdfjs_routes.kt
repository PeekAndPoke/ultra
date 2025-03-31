package de.peekandpoke.kraft.examples.jsaddons.pdfjs

import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

class PdfJsRoutes {
    val index = Static("/example/pdfjs")

    val paged = Route1("/example/pdfjs/paged/{uri}")
    fun paged(uri: String) = paged.build(uri)

    val scrolling = Route1("/example/pdfjs/scrolling/{uri}")
    fun scrolling(uri: String) = scrolling.build(uri)
}

fun RouterBuilder.mount(routes: PdfJsRoutes) {
    mount(routes.index) { PdfJsExample() }
    mount(routes.paged) { PdfJsPagedViewerExample(it["uri"]) }
    mount(routes.scrolling) { PdfJsScrollingViewerExample(it["uri"]) }
}
