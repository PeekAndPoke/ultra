package de.peekandpoke.funktor.staticweb.semanticui.templates

import de.peekandpoke.funktor.staticweb.templating.SimpleTemplateBase
import de.peekandpoke.funktor.staticweb.templating.TemplateTools
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.server.html.*
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.meta
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

open class SemanticUiAdminTemplate(
    tools: TemplateTools,
) : SimpleTemplateBase(tools) {

    init {
        pageHead {
            title { +"Admin" }
        }
    }

    override fun render(html: HTML): Unit = with(html) {
        head {
            meta { charset = "utf-8" }
            each(pageHead) { insert(it) }
            each(styles) { insert(it) }

            style("text/css") {
                unsafe {
                    +"""
                        .pusher {
                            padding-left: 260px;
                            transform: none !important;
                        }
                    """.trimIndent()
                }
            }
        }

        body {
            ui.sidebar.vertical.left.inverted.mainMenuBgColor().menu.visible.fixed {
                insert(mainMenu)
            }

            ui.pusher.padded.right {

                flashSessionEntries.takeIf { it.isNotEmpty() }?.let { entries ->
                    ui.padded.basic.segment {
                        entries.forEach { entry ->
                            ui.message.with(entry.type) {
                                +entry.message
                            }
                        }
                    }
                }

                ui.padded.basic.segment {
                    each(content) { insert(it) }
                }
            }

            each(scripts) { insert(it) }
        }
    }
}
