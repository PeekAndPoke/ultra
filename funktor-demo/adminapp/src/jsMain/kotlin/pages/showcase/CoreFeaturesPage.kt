package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoRequest
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoResponse
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.CoreFeaturesPage() = comp {
    CoreFeaturesPage(it)
}

class CoreFeaturesPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var retryResult: RetryDemoResponse? by value(null)
    private var retryRunning: Boolean by value(false)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Core Features" }
            ui.dividing.header H3 {
                +"Funktor core module capabilities — live data from the running server"
            }
        }

        renderRetryDemo()
    }

    private fun FlowContent.renderRetryDemo() {
        ui.segment {
            ui.header H2 {
                icon.redo()
                noui.content { +"Retry Mechanism Demo" }
            }

            ui.message {
                +"Click the button to simulate a retry scenario. "
                +"The operation fails on the first 2 attempts and succeeds on the 3rd."
            }

            ui.button.apply {
                if (retryRunning) disabled()
            }.then {
                onClick {
                    retryRunning = true
                    retryResult = null
                    launch {
                        retryResult = Apis.showcase.postRetryDemo(
                            RetryDemoRequest(maxAttempts = 5, failUntilAttempt = 3, delayMs = 200)
                        ).first().data
                        retryRunning = false
                    }
                }
                if (retryRunning) {
                    icon.spinner.loading()
                    +"Running..."
                } else {
                    icon.play()
                    +"Run Retry Demo"
                }
            }

            retryResult?.let { result ->
                ui.divider()

                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Attempt" }
                            th { +"Result" }
                            th { +"Message" }
                            th { +"Duration" }
                        }
                    }
                    tbody {
                        result.attempts.forEach { attempt ->
                            tr {
                                td { +"${attempt.attempt}" }
                                td {
                                    if (attempt.succeeded) {
                                        icon.green.check_circle()
                                        +"Success"
                                    } else {
                                        icon.red.times_circle()
                                        +"Failed"
                                    }
                                }
                                td { +attempt.message }
                                td { +"${attempt.durationMs} ms" }
                            }
                        }
                    }
                }

                if (result.finalSuccess) {
                    ui.positive.message { +"Operation succeeded after ${result.attempts.size} attempt(s)" }
                } else {
                    ui.negative.message { +"Operation failed after ${result.attempts.size} attempt(s)" }
                }
            }
        }
    }
}
