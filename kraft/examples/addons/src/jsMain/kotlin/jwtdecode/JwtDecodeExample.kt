@file:Suppress("detekt:all")

package io.peekandpoke.kraft.examples.jsaddons.jwtdecode

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.jwtdecode.JwtDecodeAddon
import io.peekandpoke.kraft.addons.jwtdecode.jwtDecode
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.pre

@Suppress("FunctionName")
fun Tag.JwtDecodeExample() = comp {
    JwtDecodeExample(it)
}

class JwtDecodeExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // <CodeBlock subscribing>
    private val jwtDecodeAddon: JwtDecodeAddon? by subscribingTo(addons.jwtDecode)
    // </CodeBlock>

    private var jwt by value(
        " eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.cThIIoDvwdueQB468K5xDc5633seEFoqwxjF_xSJyQQ"
    )

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val addon = jwtDecodeAddon

        ui.segment {
            ui.header H2 { +"jwt_decode (via AddonRegistry)" }

            if (addon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading jwt-decode addon..."
                    }
                }
                return@segment
            }

            ui.dividing.header { +"Subscribing to the addon" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.jwtdecode_JwtDecodeExample_kt_subscribing,
            ) {
                ui.label { +"JwtDecodeAddon is loaded via subscribingTo(addons.jwtDecode)" }
            }

            ui.dividing.header { +"Usage" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.jwtdecode_JwtDecodeExample_kt_usage,
            ) {
                // <CodeBlock usage>
                ui.form {
                    UiTextArea(jwt, { jwt = it }) {
                        label("JWT")
                    }
                }

                ui.divider()

                pre {
                    try {
                        +JSON.stringify(addon.decodeJwt(jwt))
                    } catch (e: Throwable) {
                        +e.stackTraceToString()
                    }
                }
                // </CodeBlock>
            }
        }
    }
}
