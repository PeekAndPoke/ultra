package de.peekandpoke.kraft.examples.jsaddons.jwtdecode

import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.jsbridges.decodeJwt
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.pre

@Suppress("FunctionName")
fun Tag.JwtDecodeExample() = comp {
    JwtDecodeExample(it)
}

class JwtDecodeExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var jwt by value(
        " eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.cThIIoDvwdueQB468K5xDc5633seEFoqwxjF_xSJyQQ"
    )

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"jwt_decode" }

            ui.two.column.grid {
                ui.column {
                    ui.form {
                        UiTextArea(jwt, { jwt = it }) {
                            label("JWT")
                        }
                    }
                }
                ui.column {
                    pre {
                        try {
                            +JSON.stringify(decodeJwt(jwt))
                        } catch (e: Throwable) {
                            +e.stackTraceToString()
                        }
                    }
                }
            }
        }
    }
}
