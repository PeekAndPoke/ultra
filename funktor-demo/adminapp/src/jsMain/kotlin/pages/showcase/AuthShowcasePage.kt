package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.AuthRealmInfo
import io.peekandpoke.funktor.demo.common.showcase.AuthRuleCheckResult
import io.peekandpoke.funktor.demo.common.showcase.PasswordValidationRequest
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.w3c.dom.HTMLInputElement

@Suppress("FunctionName")
fun Tag.AuthShowcasePage() = comp {
    AuthShowcasePage(it)
}

class AuthShowcasePage(ctx: NoProps) : PureComponent(ctx) {

    private var realms: List<AuthRealmInfo> by value(emptyList())
    private var ruleChecks: List<AuthRuleCheckResult> by value(emptyList())
    private var passwordInput: String by value("")
    private var passwordValid: Boolean? by value(null)
    private var policyDescription: String by value("")

    init {
        launch { realms = Apis.showcase.getAuthRealms().first().data ?: emptyList() }
        launch { ruleChecks = Apis.showcase.getAuthRuleChecks().first().data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Auth & Authorization" }
            ui.dividing.header H3 { +"Authentication realms, password policy, and authorization rules" }
        }

        renderRealms()
        renderPasswordPolicy()
        renderAuthRules()
    }

    private fun FlowContent.renderRealms() {
        ui.segment {
            ui.header H2 {
                icon.shield_alternate()
                noui.content { +"Auth Realms" }
            }

            if (realms.isEmpty()) {
                ui.placeholder.segment { ui.header { +"Loading..." } }
            } else {
                realms.forEach { realm ->
                    ui.segment {
                        ui.header H3 { +"Realm: ${realm.id}" }

                        ui.striped.table Table {
                            thead { tr { th { +"Provider" }; th { +"Type" }; th { +"Capabilities" } } }
                            tbody {
                                realm.providers.forEach { provider ->
                                    tr {
                                        td { +provider.id }
                                        td { ui.label { +provider.type } }
                                        td { provider.capabilities.forEach { cap -> ui.label { +cap } } }
                                    }
                                }
                            }
                        }

                        ui.message {
                            ui.header { +"Password Policy" }
                            +realm.passwordPolicyDescription
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderPasswordPolicy() {
        ui.segment {
            ui.header H2 {
                icon.key()
                noui.content { +"Password Policy Validator" }
            }

            ui.form {
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Password" }
                        input(type = InputType.text) {
                            value = passwordInput
                            onChange { evt ->
                                passwordInput = (evt.target as HTMLInputElement).value
                                launch {
                                    val resp = Apis.showcase.validatePassword(
                                        PasswordValidationRequest(password = passwordInput)
                                    ).first().data
                                    passwordValid = resp?.matches
                                    policyDescription = resp?.policyDescription ?: ""
                                }
                            }
                        }
                    }
                }
            }

            if (passwordInput.isNotEmpty()) {
                when (passwordValid) {
                    true -> ui.positive.message { icon.check_circle(); +"Password meets policy requirements" }
                    false -> ui.negative.message { icon.times_circle(); +"Password does not meet policy: $policyDescription" }
                    null -> {}
                }
            }
        }
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
