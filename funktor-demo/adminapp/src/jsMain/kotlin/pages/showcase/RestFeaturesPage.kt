package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.EchoResponse
import io.peekandpoke.funktor.demo.common.showcase.ItemResponse
import io.peekandpoke.funktor.demo.common.showcase.ServerTimeResponse
import io.peekandpoke.funktor.demo.common.showcase.TransformRequest
import io.peekandpoke.funktor.demo.common.showcase.TransformResponse
import io.peekandpoke.funktor.demo.common.showcase.UpdateItemRequest
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
import kotlinx.html.option
import kotlinx.html.pre
import kotlinx.html.select
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement

@Suppress("FunctionName")
fun Tag.RestFeaturesPage() = comp {
    RestFeaturesPage(it)
}

class RestFeaturesPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // Plain route demo
    private var plainResult: ServerTimeResponse? by value(null)

    // Echo route demo
    private var echoMessage: String by value("Hello Funktor!")
    private var echoResult: EchoResponse? by value(null)

    // Transform route demo
    private var transformText: String by value("hello world")
    private var transformOp: String by value("uppercase")
    private var transformResult: TransformResponse? by value(null)

    // Item route demo
    private var itemId: String by value("item-42")
    private var itemName: String by value("My Updated Item")
    private var itemResult: ItemResponse? by value(null)

    //  INIT  ///////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"REST API Explorer" }
            ui.dividing.header H3 {
                +"Demonstrates typed API routes and interactive 'Try It' forms"
            }
        }

        renderPlainRouteDemo()
        renderEchoRouteDemo()
        renderTransformRouteDemo()
        renderItemRouteDemo()
    }

    private fun FlowContent.renderPlainRouteDemo() {
        ui.segment {
            ui.header H2 {
                icon.arrow_right()
                noui.content { +"ApiRoute.Plain — GET without parameters" }
            }

            ui.message {
                pre { +"GET /showcase/rest/plain\n\nReturns the current server time. No parameters needed." }
            }

            ui.button {
                onClick {
                    launch {
                        plainResult = Apis.showcase.getPlain().first().data
                    }
                }
                icon.play()
                +"Try It"
            }

            plainResult?.let { result ->
                ui.divider()
                renderJsonResult("""{ "timestamp": "${result.timestamp}", "epochMs": ${result.epochMs} }""")
            }
        }
    }

    private fun FlowContent.renderEchoRouteDemo() {
        ui.segment {
            ui.header H2 {
                icon.arrow_right()
                noui.content { +"ApiRoute.WithParams — GET with typed path parameter" }
            }

            ui.message {
                pre { +"GET /showcase/rest/echo/{message}\n\nEchoes the message from the path parameter." }
            }

            ui.form {
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Message" }
                        input(type = InputType.text) {
                            value = echoMessage
                            onChange { echoMessage = (it.target as HTMLInputElement).value }
                        }
                    }
                }
            }

            ui.button {
                onClick {
                    launch {
                        echoResult = Apis.showcase.getEcho(echoMessage).first().data
                    }
                }
                icon.play()
                +"Try It"
            }

            echoResult?.let { result ->
                ui.divider()
                renderJsonResult("""{ "message": "${result.message}", "echoedAt": "${result.echoedAt}" }""")
            }
        }
    }

    private fun FlowContent.renderTransformRouteDemo() {
        ui.segment {
            ui.header H2 {
                icon.arrow_right()
                noui.content { +"ApiRoute.WithBody — POST with request body" }
            }

            ui.message {
                pre { +"POST /showcase/rest/transform\n\nAccepts a JSON body with text and operation, returns the transformed text." }
            }

            ui.form {
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Text" }
                        input(type = InputType.text) {
                            value = transformText
                            onChange { transformText = (it.target as HTMLInputElement).value }
                        }
                    }
                }
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Operation" }
                        select {
                            onChange { transformOp = (it.target as HTMLSelectElement).value }
                            option { value = "uppercase"; +"uppercase" }
                            option { value = "lowercase"; +"lowercase" }
                            option { value = "reverse"; +"reverse" }
                            option { value = "base64"; +"base64" }
                        }
                    }
                }
            }

            ui.button {
                onClick {
                    launch {
                        transformResult = Apis.showcase.postTransform(
                            TransformRequest(text = transformText, operation = transformOp)
                        ).first().data
                    }
                }
                icon.play()
                +"Try It"
            }

            transformResult?.let { result ->
                ui.divider()
                renderJsonResult(
                    """{ "original": "${result.original}", "transformed": "${result.transformed}", "operation": "${result.operation}" }"""
                )
            }
        }
    }

    private fun FlowContent.renderItemRouteDemo() {
        ui.segment {
            ui.header H2 {
                icon.arrow_right()
                noui.content { +"ApiRoute.WithBodyAndParams — PUT with path param + body" }
            }

            ui.message {
                pre { +"PUT /showcase/rest/items/{id}\n\nCombines a path parameter (id) with a JSON request body (name)." }
            }

            ui.form {
                noui.two.fields {
                    noui.field {
                        ui.labeled.input {
                            ui.label { +"ID" }
                            input(type = InputType.text) {
                                value = itemId
                                onChange { itemId = (it.target as HTMLInputElement).value }
                            }
                        }
                    }
                    noui.field {
                        ui.labeled.input {
                            ui.label { +"Name" }
                            input(type = InputType.text) {
                                value = itemName
                                onChange { itemName = (it.target as HTMLInputElement).value }
                            }
                        }
                    }
                }
            }

            ui.button {
                onClick {
                    launch {
                        itemResult = Apis.showcase.putItem(
                            id = itemId,
                            request = UpdateItemRequest(name = itemName)
                        ).first().data
                    }
                }
                icon.play()
                +"Try It"
            }

            itemResult?.let { result ->
                ui.divider()
                renderJsonResult(
                    """{ "id": "${result.id}", "name": "${result.name}", "updatedAt": "${result.updatedAt}" }"""
                )
            }
        }
    }

    private fun FlowContent.renderJsonResult(json: String) {
        ui.segment {
            ui.label {
                icon.check_circle()
                +"Response"
            }
            pre { +json }
        }
    }
}
