package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.BackgroundRepeat
import kotlinx.css.Color
import kotlinx.css.Image
import kotlinx.css.Margin
import kotlinx.css.Padding
import kotlinx.css.Position
import kotlinx.css.RelativePosition
import kotlinx.css.backdropFilter
import kotlinx.css.background
import kotlinx.css.backgroundColor
import kotlinx.css.backgroundImage
import kotlinx.css.backgroundPosition
import kotlinx.css.backgroundRepeat
import kotlinx.css.backgroundSize
import kotlinx.css.bottom
import kotlinx.css.height
import kotlinx.css.left
import kotlinx.css.margin
import kotlinx.css.padding
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.right
import kotlinx.css.top
import kotlinx.css.vh
import kotlinx.css.width
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.div

object AuthPageLayouts {
    operator fun <T> invoke(block: AuthPageLayouts.() -> T) = block(this)

    fun FlowContent.renderFullscreenBackgroundLayout(config: AuthFrontendConfig, block: DIV.() -> Unit) {
        div {
            css {
                position = Position.relative
                padding = Padding(0.px)
                margin = Margin(0.px)
                height = 100.vh
            }

            renderBackgroundImage(config)

            ui.very.padded.attached.segment {
                css {
                    background = Color.white.withAlpha(0.9).toString()
                    backdropFilter = "blur(5px)"
                    height = 100.vh
                    width = 480.px
                }

                block()
            }
        }
    }

    fun FlowContent.renderBackgroundImage(config: AuthFrontendConfig) {
        div {
            css {
                position = Position.absolute
                top = 0.px
                left = 0.px
                bottom = 0.px
                right = 0.px

                when (val bgUrl = config.backgroundImageUrl) {
                    null -> {
                        backgroundColor = Color.grey
                    }

                    else -> {
                        backgroundImage = Image("url($bgUrl)")
                        backgroundSize = "cover"
                        backgroundPosition = RelativePosition.center
                        backgroundRepeat = BackgroundRepeat.noRepeat
                    }
                }
            }
        }
    }
}
