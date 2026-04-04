@file:Suppress("detekt:all")

package io.peekandpoke.kraft.examples.jsaddons

import io.peekandpoke.kraft.addons.avatars.AvatarsAddon
import io.peekandpoke.kraft.addons.avatars.avatars
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.core.CoreExamples
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.height
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.HomePage() = comp {
    HomePage(it)
}

class HomePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val avatarsAddon: AvatarsAddon? by subscribingTo(addons.avatars)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        renderAddonCards()

        ui.divider()

        renderCoreCards()
    }

    private fun FlowContent.renderAddonCards() {
        ui.four.doubling.link.cards {

            noui.card A {
                href(routes.avatars.index())

                noui.center.aligned.content {
                    val avatarSrc = avatarsAddon?.getDataUrl("alienHead66.svg") ?: ""
                    renderLogo(
                        src = avatarSrc,
                        alt = "Avatars",
                    )
                }
                noui.center.aligned.content {
                    +"Avatars"
                }
            }

            noui.card A {
                href(routes.browserDetect())

                noui.center.aligned.content {
                    renderLogo(
                        src = "images/browsers.jpg",
                        alt = "Browser Detect",
                    )
                }
                noui.center.aligned.content {
                    +"Browser Detect"
                }
            }

            noui.card A {
                href(routes.chartJs())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://www.chartjs.org/img/chartjs-logo.svg",
                        alt = "ChartJS Logo",
                    )
                }
                noui.center.aligned.content {
                    +"ChartJS"
                }
            }

            noui.card A {
                href(routes.jwtDecode())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://user-images.githubusercontent.com/83319/31722733-de95bbde-b3ea-11e7-96bf-4f4e8f915588.png",
                        alt = "JWT Decode Logo",
                    )
                }
                noui.center.aligned.content {
                    +"JWT Decode"
                }
            }


            noui.card A {
                href(routes.marked())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://marked.js.org/img/logo-black.svg",
                        alt = "Marked Logo",
                    )
                }
                noui.center.aligned.content {
                    +"Marked"
                }
            }

            noui.card A {
                href(routes.pdfjs.index())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Pdf-js_logo.svg/800px-Pdf-js_logo.svg.png",
                        alt = "PDF JS",
                    )
                }
                noui.center.aligned.content {
                    +"PDF JS"
                }
            }

            noui.card A {
                href(routes.prismjs())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://pbs.twimg.com/profile_images/2451426554/Screen_Shot_2012-07-31_at_21.57.03__400x400.png",
                        alt = "PrismJs Logo",
                    )
                }
                noui.center.aligned.content {
                    +"PrismJs"
                }
            }

            noui.card A {
                href(routes.signaturePad())

                noui.center.aligned.content {
                    renderLogo(
                        src = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/56/Autograph_of_Benjamin_Franklin.svg/1920px-Autograph_of_Benjamin_Franklin.svg.png",
                        alt = "Signature Pad",
                    )
                }
                noui.center.aligned.content {
                    +"Signature Pad"
                }
            }
        }
    }

    private fun FlowContent.renderCoreCards() {
        CoreExamples()
    }

    private fun FlowContent.renderLogo(src: String, alt: String) {
        ui.image Img {
            css {
                height = 100.px
            }
            this.src = src
            this.alt = alt
        }
    }
}
