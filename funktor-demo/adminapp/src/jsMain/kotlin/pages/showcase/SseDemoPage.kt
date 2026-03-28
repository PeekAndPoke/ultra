package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.common.showcase.SseClockEvent
import io.peekandpoke.funktor.demo.common.showcase.SseMetricsEvent
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.serialization.json.Json

@Suppress("FunctionName")
fun Tag.SseDemoPage() = comp {
    SseDemoPage(it)
}

class SseDemoPage(ctx: NoProps) : PureComponent(ctx) {

    private var clockEvent: SseClockEvent? by value(null)
    private var metricsEvent: SseMetricsEvent? by value(null)
    private var clockConnected: Boolean by value(false)
    private var metricsConnected: Boolean by value(false)

    private val json = Json { ignoreUnknownKeys = true }

    // Note: SSE connections from Kotlin/JS would use EventSource API
    // For now, show the feature description and endpoint info

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Server-Sent Events (SSE)" }
            ui.dividing.header H3 { +"Real-time streaming from server to client" }
        }

        renderClockDemo()
        renderMetricsDemo()
    }

    private fun FlowContent.renderClockDemo() {
        ui.segment {
            ui.header H2 { icon.clock(); noui.content { +"Server Clock Stream" } }

            ui.message {
                +"SSE endpoint: "
                ui.label { +"GET /showcase/realtime/sse/clock" }
                +" streams the server time every second."
            }

            ui.info.message {
                ui.header { +"ApiRoute.Sse" }
                +"This endpoint uses the "
                ui.label { +"ApiRoute.Sse" }
                +" route type, which provides server-to-client event streaming over HTTP."
            }

            clockEvent?.let { event ->
                ui.statistic {
                    noui.value { +event.timestamp }
                    noui.label { +"Server Time" }
                }
            }
        }
    }

    private fun FlowContent.renderMetricsDemo() {
        ui.segment {
            ui.header H2 { icon.chart_bar(); noui.content { +"JVM Metrics Stream" } }

            ui.message {
                +"SSE endpoint: "
                ui.label { +"GET /showcase/realtime/sse/metrics" }
                +" streams JVM metrics every 2 seconds."
            }

            metricsEvent?.let { event ->
                ui.four.statistics {
                    ui.statistic { noui.value { +"${event.heapUsedMb}" }; noui.label { +"Heap Used (MB)" } }
                    ui.statistic { noui.value { +"${event.heapMaxMb}" }; noui.label { +"Heap Max (MB)" } }
                    ui.statistic { noui.value { +"${event.threadCount}" }; noui.label { +"Threads" } }
                    ui.statistic { noui.value { +"${event.uptimeSeconds}" }; noui.label { +"Uptime (s)" } }
                }
            } ?: run {
                ui.placeholder.segment {
                    ui.header { +"Connect to the SSE endpoint to see live metrics" }
                }
            }
        }
    }
}
