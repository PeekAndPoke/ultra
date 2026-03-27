package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.CliCommandInfo
import io.peekandpoke.funktor.demo.common.showcase.ConfigInfoEntry
import io.peekandpoke.funktor.demo.common.showcase.FixtureInfo
import io.peekandpoke.funktor.demo.common.showcase.LifecycleHookInfo
import io.peekandpoke.funktor.demo.common.showcase.RepairInfo
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

    private var lifecycleHooks: List<LifecycleHookInfo> by value(emptyList())
    private var configInfo: List<ConfigInfoEntry> by value(emptyList())
    private var cliCommands: List<CliCommandInfo> by value(emptyList())
    private var fixtures: List<FixtureInfo> by value(emptyList())
    private var repairs: List<RepairInfo> by value(emptyList())
    private var retryResult: RetryDemoResponse? by value(null)
    private var retryRunning: Boolean by value(false)

    //  INIT  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            lifecycleHooks = Apis.showcase.getLifecycleHooks().first().data ?: emptyList()
        }
        launch {
            configInfo = Apis.showcase.getConfigInfo().first().data ?: emptyList()
        }
        launch {
            cliCommands = Apis.showcase.getCliCommands().first().data ?: emptyList()
        }
        launch {
            fixtures = Apis.showcase.getFixtures().first().data ?: emptyList()
        }
        launch {
            repairs = Apis.showcase.getRepairs().first().data ?: emptyList()
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Core Features" }
            ui.dividing.header H3 {
                +"Funktor core module capabilities — live data from the running server"
            }
        }

        renderLifecycleHooks()
        renderConfigInfo()
        renderCliCommands()
        renderFixtures()
        renderRepairs()
        renderRetryDemo()
    }

    private fun FlowContent.renderLifecycleHooks() {
        ui.segment {
            ui.header H2 {
                icon.heartbeat()
                noui.content { +"Lifecycle Hooks" }
            }

            if (lifecycleHooks.isEmpty()) {
                ui.placeholder.segment { ui.header { +"Loading..." } }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Phase" }
                            th { +"Hook Class" }
                            th { +"Execution Order" }
                        }
                    }
                    tbody {
                        lifecycleHooks.forEach { hook ->
                            tr {
                                td { +hook.phase }
                                td { +hook.className }
                                td { +"${hook.executionOrder}" }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderConfigInfo() {
        ui.segment {
            ui.header H2 {
                icon.cog()
                noui.content { +"Configuration" }
            }

            if (configInfo.isEmpty()) {
                ui.placeholder.segment { ui.header { +"Loading..." } }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Key" }
                            th { +"Value" }
                        }
                    }
                    tbody {
                        configInfo.forEach { entry ->
                            tr {
                                td { +entry.key }
                                td { +entry.value }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderCliCommands() {
        ui.segment {
            ui.header H2 {
                icon.terminal()
                noui.content { +"CLI Commands" }
            }

            if (cliCommands.isEmpty()) {
                ui.placeholder.segment { ui.header { +"No CLI commands registered" } }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Command" }
                            th { +"Help" }
                        }
                    }
                    tbody {
                        cliCommands.forEach { cmd ->
                            tr {
                                td {
                                    ui.label { +cmd.name }
                                }
                                td { +cmd.help }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderFixtures() {
        ui.segment {
            ui.header H2 {
                icon.database()
                noui.content { +"Fixture Loaders" }
            }

            if (fixtures.isEmpty()) {
                ui.placeholder.segment { ui.header { +"No fixtures registered" } }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Fixture Class" }
                            th { +"Depends On" }
                        }
                    }
                    tbody {
                        fixtures.forEach { fix ->
                            tr {
                                td { +fix.className }
                                td {
                                    if (fix.dependsOn.isEmpty()) {
                                        +"(none)"
                                    } else {
                                        fix.dependsOn.forEach { dep ->
                                            ui.label { +dep }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderRepairs() {
        ui.segment {
            ui.header H2 {
                icon.wrench()
                noui.content { +"Repair System" }
            }

            if (repairs.isEmpty()) {
                ui.placeholder.segment { ui.header { +"No repairs registered" } }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Repair Class" }
                        }
                    }
                    tbody {
                        repairs.forEach { repair ->
                            tr {
                                td { +repair.className }
                            }
                        }
                    }
                }
            }
        }
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
