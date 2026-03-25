package io.peekandpoke.kraft.examples.jsaddons.pdfjs

import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.Static

class PdfJsRoutes {
    val index = Static("/example/pdfjs")

    val paged = Route1("/example/pdfjs/paged/{uri}")
    fun paged(uri: String) = paged.bind(uri)

    val scrolling = Route1("/example/pdfjs/scrolling/{uri}")
    fun scrolling(uri: String) = scrolling.bind(uri)
}

fun RootRouterBuilder.mount(routes: PdfJsRoutes) {
    mount(routes.index) { PdfJsExample() }
    mount(routes.paged) { PdfJsPagedViewerExample(it["uri"]) }
    mount(routes.scrolling) { PdfJsScrollingViewerExample(it["uri"]) }
}
