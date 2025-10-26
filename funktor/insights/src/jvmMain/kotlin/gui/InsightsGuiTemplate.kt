package de.peekandpoke.funktor.insights.gui

import de.peekandpoke.funktor.core.broker.TypedRoute
import de.peekandpoke.funktor.core.broker.TypedRouteRenderer
import de.peekandpoke.funktor.insights.InsightsMapper
import de.peekandpoke.funktor.insights.collectors.RuntimeCollector
import de.peekandpoke.funktor.insights.collectors.VaultCollector
import de.peekandpoke.funktor.staticweb.resources.WebResources
import de.peekandpoke.funktor.staticweb.resources.common.fomanticUi
import de.peekandpoke.funktor.staticweb.resources.common.jQuery
import de.peekandpoke.funktor.staticweb.resources.common.vizJs
import de.peekandpoke.funktor.staticweb.resources.css
import de.peekandpoke.funktor.staticweb.resources.js
import de.peekandpoke.funktor.staticweb.resources.prismjs.Language
import de.peekandpoke.funktor.staticweb.resources.prismjs.prism
import de.peekandpoke.funktor.staticweb.resources.prismjs.prismJs
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.semantic
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.http.*
import io.ktor.server.html.*
import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.style
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import kotlinx.html.unsafe

class InsightsGuiTemplate(
    val kontainer: Kontainer,
    private val routes: InsightsGuiRoutes,
    private val routesRenderer: TypedRouteRenderer,
    private val webResources: WebResources,
    private val mapper: InsightsMapper,
) : Template<HTML> {

    val styles = PlaceholderList<HEAD, HEAD>()
    val scripts = PlaceholderList<FlowContent, FlowContent>()

    val bodyPlaceholder = Placeholder<BODY>()

    val menuPlaceholders = PlaceholderList<FlowContent, FlowContent>()

    val contentPlaceholders = PlaceholderList<FlowContent, FlowContent>()

    /**
     * Renders the url of a bound typed route
     */
    val <PARAMS : Any> TypedRoute.Bound<PARAMS>.url get() = routesRenderer.render(this)

    init {
        styles {
            css(webResources.jQuery)
            css(webResources.fomanticUi)
            css(webResources.prismJs)
            css(webResources.insightsGui)
        }

        scripts {
            js(webResources.jQuery)
            js(webResources.fomanticUi)
            js(webResources.prismJs)
            js(webResources.vizJs)
            js(webResources.insightsGui)
        }

        menuPlaceholders {
            ui.item.active {
                attributes["data-key"] = "overview"

                icon.chart_bar_outline()
                +"Overview"
            }
        }
    }

    override fun HTML.apply() {

        head {
            each(styles) { insert(it) }

            style {
                unsafe {
                    +"""
                        body {
                            overflow-x: auto !important;
                        }                    
                    """.trimIndent()
                }
            }
        }

        body {
            insert(bodyPlaceholder)

            each(scripts) { insert(it) }
        }
    }

    fun render(guiData: InsightsGuiData) {
        contentPlaceholders {
            div {
                attributes["data-key"] = "overview"

                ui.basic.segment {

                    ui.segment {
                        ui.header H3 { +"Overview" }

                        ui.basic.table Table {
                            tbody {
                                tr {
                                    td { +"Request" }
                                    td { +(guiData.requestMethod + " " + guiData.requestUrl) }
                                }
                                tr {
                                    td { +"Status" }
                                    td {
                                        val status = guiData.statusCode
                                        when {
                                            status == null -> ui.grey.label { +"n/a" }
                                            status.isSuccess() -> ui.green.label { +"${status.value} ${status.description}" }
                                            else -> ui.red.label { +"${status.value} ${status.description}" }
                                        }
                                    }
                                }
                                tr {
                                    td { +"Response time" }
                                    td { +guiData.responseTimeMs }
                                }
                                tr {
                                    td { +"Timestamp" }
                                    td { +guiData.ts.toString() }
                                }
                            }
                        }
                    }

                    guiData.use(VaultCollector.Data::class) {
                        ui.segment {
                            ui.header H3 { +"Database" }
                            stats()
                        }
                    }

                    guiData.use(RuntimeCollector.Data::class) {
                        ui.segment {
                            ui.header H3 { +"Runtime" }
                            stats()
                        }
                    }
                }
            }
        }

        guiData.collectors.forEach {
            it.renderDetails(this)
        }

        bodyPlaceholder {

            val coloring = when {
                guiData.statusCode?.isSuccess() == true -> semantic { green.inverted }
                guiData.statusCode?.isSuccess() == false -> semantic { red.inverted }
                else -> semantic { yellow.inverted }
            }

            ui.coloring().attached.segment {

                ui.big.horizontal.divided.list {
                    ui.inverted.header.item { +"Funktor" }

                    ui.item { +guiData.statusCode.toString() }
                    ui.item { +guiData.requestMethod }
                    ui.item { +guiData.requestUrl }
                    ui.item { +guiData.responseTimeMs }
                }
            }

            ui.attached.inverted.labeled.tiny.icon.menu {
                id = "menu"

                each(menuPlaceholders) {
                    insert(it)
                }
            }

            div {
                id = "content"

                each(contentPlaceholders) {
                    insert(it)
                }
            }
        }
    }

    fun FlowContent.json(data: Any?) {
        prism(Language.Json) {
            prettyPrintJson(data)
        }
    }

    fun prettyPrintJson(data: Any?): String {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data) ?: ""
    }
}
