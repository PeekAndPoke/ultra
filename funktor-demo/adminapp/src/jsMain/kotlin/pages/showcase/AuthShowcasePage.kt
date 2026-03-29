package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.AuthRuleCheckResult
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
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
fun Tag.AuthShowcasePage() = comp {
    AuthShowcasePage(it)
}

class AuthShowcasePage(ctx: NoProps) : PureComponent(ctx) {

    private var ruleChecks: List<AuthRuleCheckResult> by value(emptyList())

    init {
        launch { ruleChecks = Apis.showcase.getAuthRuleChecks().first().data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Auth & Authorization" }
            ui.dividing.header H3 { +"Authorization rules check for the current user" }
        }

        renderAuthRules()
    }

    private fun FlowContent.renderAuthRules() {
        ui.segment {
            ui.header H2 {
                icon.lock()
                noui.content { +"Authorization Rules Check" }
            }

            ui.message { +"Shows which authorization rules your current user passes." }

            if (ruleChecks.isEmpty()) {
                ui.placeholder.segment { ui.header { +"Loading..." } }
            } else {
                ui.striped.table Table {
                    thead { tr { th { +"Rule" }; th { +"Description" }; th { +"Access" } } }
                    tbody {
                        ruleChecks.forEach { check ->
                            tr {
                                td { ui.label { +check.ruleName } }
                                td { +check.description }
                                td {
                                    if (check.accessGranted) {
                                        icon.green.check_circle(); +"Granted"
                                    } else {
                                        icon.red.times_circle(); +"Denied"
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
