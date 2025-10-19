package de.peekandpoke.funktor.demo.adminapp.pages

import de.peekandpoke.funktor.auth.widgets.LoginWidget
import de.peekandpoke.funktor.demo.adminapp.Nav
import de.peekandpoke.funktor.demo.adminapp.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Align
import kotlinx.css.BackgroundRepeat
import kotlinx.css.Display
import kotlinx.css.Image
import kotlinx.css.JustifyContent
import kotlinx.css.RelativePosition
import kotlinx.css.alignItems
import kotlinx.css.backgroundImage
import kotlinx.css.backgroundPosition
import kotlinx.css.backgroundRepeat
import kotlinx.css.backgroundSize
import kotlinx.css.display
import kotlinx.css.height
import kotlinx.css.justifyContent
import kotlinx.css.maxWidth
import kotlinx.css.opacity
import kotlinx.css.pct
import kotlinx.css.px
import kotlinx.css.vh
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoginPage() = comp {
    LoginPage(it)
}

class LoginPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {

        div {
            css {
                height = 100.vh

                backgroundImage = Image(
                    "url(https://miro.medium.com/v2/resize:fit:2048/format:webp/0*m_7JMnJZnFN2338H.png)"
                )

                backgroundSize = "cover"
                backgroundPosition = RelativePosition.center
                backgroundRepeat = BackgroundRepeat.noRepeat
            }

//            div(classes = "background") {
//                css {
//                    position = Position.absolute
//                    top = 0.px
//                    bottom = 0.px
//                    left = 0.px
//                    right = 0.px
//                }
//
//                canvas {
//                    css {
//                        width = 100.pct
//                        height = 100.pct
//                    }
//                }
//            }


            ui.container {
                css {
                    height = 100.vh
                    display = Display.flex
                    alignItems = Align.center
                    justifyContent = JustifyContent.center
                }

                ui.segment {
                    css {
                        width = 100.pct
                        maxWidth = 800.px
                        opacity = 0.95
                    }

                    LoginWidget(
                        state = State.auth,
                        onLoginSuccessUri = Nav.dashboard(),
                    )
                }
            }
        }
    }
}
