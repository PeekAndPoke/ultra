package io.peekandpoke.kraft.examples.fomanticui.helpers

import io.peekandpoke.kraft.addons.prismjs.PrismHtml
import io.peekandpoke.kraft.addons.prismjs.PrismKotlin
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.ShowLanguage.Companion.showLanguage
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.p

fun FlowContent.readTheFomanticUiDocs(url: String) {
    ui.positive.message {
        icon.book()

        a(href = url, target = "_blank") {
            +"Read the docs in Fomantic-UI at $url"
        }
    }
}

fun FlowContent.example(block: DIV.() -> Unit) {
    div("example") {
        block()
    }
}

fun FlowContent.shortParagraphWireFrame() {
    p {
        ui.wireframe.image Img {
            src = "images/wireframe/short-paragraph.png"
        }
    }
}

fun FlowContent.mediaParagraphWireFrame() {
    p {
        ui.wireframe.image Img {
            src = "images/wireframe/media-paragraph.png"
        }
    }
}

fun FlowContent.kotlinToHtml(
    kotlin: String,
    html: String,
) {
    ui.segment {
        ui.two.column.very.relaxed.grid {
            noui.column {
                ui.header { +"Kotlin" }
                PrismKotlin(kotlin) {
                    lineNumbers()
                    showLanguage()
                    copyToClipboard()
                }
            }

            noui.column {
                ui.header { +"HTML" }
                PrismHtml(html) {
                    lineNumbers()
                    showLanguage()
                    copyToClipboard()
                }
            }
        }
        ui.vertical.divider { +">>" }
    }
}
