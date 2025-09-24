package de.peekandpoke.funktor.insights.gui

import de.peekandpoke.funktor.core.broker.TypedRoute
import de.peekandpoke.funktor.core.broker.TypedRouteRenderer
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.model.AppInfo
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.http.*
import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.title
import java.io.StringWriter

class InsightsBarTemplate(
    private val data: InsightsGuiRoutes.PathParam,
    private val routes: InsightsGuiRoutes,
    private val guiData: InsightsGuiData,
    private val routeRenderer: TypedRouteRenderer,
    private val appConfig: AppConfig,
    private val appInfo: AppInfo,
) {

    val status = PlaceholderList<FlowContent, FlowContent>()
    val left = PlaceholderList<FlowContent, FlowContent>()
    val right = PlaceholderList<FlowContent, FlowContent>()

    private val consumer: TagConsumer<Appendable> = StringWriter().appendHTML()

    private fun <T : Any> url(route: TypedRoute.Bound<T>) = routeRenderer.render(route)

    init {

        status {

            ui.item {
                val statusCode = guiData.statusCode

                a(href = url(routes.details(data)), target = "_blank") {
                    when {
                        statusCode == null -> ui.grey.label { +"???" }

                        statusCode.isSuccess() -> ui.green.label { +(statusCode.value).toString() }

                        else -> ui.red.label { +(statusCode.value).toString() }
                    }
                }
            }
        }

        left {

            ui.item {
                title = "Git: ${appInfo.version.gitBranch} ${appInfo.version.gitDesc}"
                icon.code_branch()
                +appInfo.version.describeGit()
            }

            ui.item {
                title = "Env: ${appConfig.ktor.application.id}"
                +appConfig.ktor.application.id
            }

            ui.item {
                title = "Processing time"

                val duration = guiData.stopWatch.totalDuration()
                val durationMillis: Double = duration.inWholeNanoseconds / 1_000_000.0

                when {
                    durationMillis > 300 -> icon.red.clock()
                    durationMillis > 150 -> icon.yellow.clock()
                    durationMillis > 75 -> icon.olive.clock()
                    else -> icon.green.clock()
                }

                +"%.2f ms".format(durationMillis)
            }
        }

        guiData.collectors.forEach {
            it.renderBar(this)
        }
    }

    fun render() = consumer.div(classes = "insights-bar") {
        id = "insights-bar"

        ui.attached.inverted.segment {

            ui.inverted.horizontal.divided.list {
                each(status) { insert(it) }
                each(left) { insert(it) }
            }

            ui.inverted.horizontal.right.aligned.list {
                each(right) { insert(it) }
            }

            span {
                id = "close-insights-bar"
                style = "float: right; margin-top: 6px;"
                icon.inverted.window_close_outline()
            }
        }
    }
}
