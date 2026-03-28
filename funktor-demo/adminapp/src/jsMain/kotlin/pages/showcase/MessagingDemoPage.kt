package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.EmailSenderInfo
import io.peekandpoke.funktor.demo.common.showcase.SendTestEmailRequest
import io.peekandpoke.funktor.demo.common.showcase.SendTestEmailResponse
import io.peekandpoke.funktor.demo.common.showcase.SentMessageInfo
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.html.onClick
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
import kotlinx.html.textArea
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

@Suppress("FunctionName")
fun Tag.MessagingDemoPage() = comp {
    MessagingDemoPage(it)
}

class MessagingDemoPage(ctx: NoProps) : PureComponent(ctx) {

    private var senderInfo: EmailSenderInfo? by value(null)
    private var sentMessages: List<SentMessageInfo> by value(emptyList())
    private var sendResult: SendTestEmailResponse? by value(null)

    private var emailTo: String by value("test@example.com")
    private var emailSubject: String by value("Test Email from Funktor Demo")
    private var emailBody: String by value("This is a test email sent from the funktor-demo showcase.")
    private var sending: Boolean by value(false)

    init {
        launch { senderInfo = Apis.showcase.getEmailSenderInfo().first().data }
        launch { sentMessages = Apis.showcase.getSentMessages().first().data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Messaging" }
            ui.dividing.header H3 { +"Email sending and sent message history" }
        }

        renderSenderInfo()
        renderComposeForm()
        renderSentMessages()
    }

    private fun FlowContent.renderSenderInfo() {
        senderInfo?.let { info ->
            ui.segment {
                ui.header H2 { icon.info_circle(); noui.content { +"Email Sender" } }
                ui.label { +"Type: ${info.type}" }
            }
        }
    }

    private fun FlowContent.renderComposeForm() {
        ui.segment {
            ui.header H2 { icon.envelope(); noui.content { +"Send Test Email" } }

            ui.form {
                noui.field {
                    ui.labeled.input {
                        ui.label { +"To" }
                        input(type = InputType.text) {
                            value = emailTo
                            onChange { emailTo = (it.target as HTMLInputElement).value }
                        }
                    }
                }
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Subject" }
                        input(type = InputType.text) {
                            value = emailSubject
                            onChange { emailSubject = (it.target as HTMLInputElement).value }
                        }
                    }
                }
                noui.field {
                    textArea {
                        +emailBody
                        onChange { emailBody = (it.target as HTMLTextAreaElement).value }
                    }
                }
            }

            ui.button.apply { if (sending) disabled() }.then {
                onClick {
                    sending = true
                    sendResult = null
                    launch {
                        sendResult = Apis.showcase.sendTestEmail(
                            SendTestEmailRequest(to = emailTo, subject = emailSubject, body = emailBody)
                        ).first().data
                        sending = false
                        sentMessages = Apis.showcase.getSentMessages().first().data ?: emptyList()
                    }
                }
                if (sending) {
                    icon.spinner.loading(); +"Sending..."
                } else {
                    icon.paper_plane(); +"Send"
                }
            }

            sendResult?.let { result ->
                ui.divider()
                if (result.success) {
                    ui.positive.message { +"Email sent successfully (ID: ${result.messageId ?: "n/a"})" }
                } else {
                    ui.negative.message { +"Failed to send: ${result.error ?: "unknown error"}" }
                }
            }
        }
    }

    private fun FlowContent.renderSentMessages() {
        ui.segment {
            ui.header H2 { icon.history(); noui.content { +"Sent Messages" } }

            ui.button {
                onClick { launch { sentMessages = Apis.showcase.getSentMessages().first().data ?: emptyList() } }
                icon.redo(); +"Refresh"
            }

            if (sentMessages.isNotEmpty()) {
                ui.striped.table Table {
                    thead { tr { th { +"To" }; th { +"Subject" }; th { +"Status" }; th { +"Sent" } } }
                    tbody {
                        sentMessages.forEach { msg ->
                            tr {
                                td { +msg.to }
                                td { +msg.subject }
                                td {
                                    if (msg.success) {
                                        icon.green.check_circle()
                                    } else {
                                        icon.red.times_circle()
                                    }
                                }
                                td { +msg.sentAt }
                            }
                        }
                    }
                }
            } else {
                ui.message { +"No sent messages yet" }
            }
        }
    }
}
