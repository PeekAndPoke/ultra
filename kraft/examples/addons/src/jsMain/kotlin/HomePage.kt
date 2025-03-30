package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.addons.avatars.Avatars
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        renderAddonCards()

        ui.divider()

        renderCoreCards()
    }

    private fun FlowContent.renderAddonCards() {
        ui.four.doubling.link.cards {

            noui.card A {
                href = routes.avatars.index()

                noui.center.aligned.content {
                    renderLogo(
                        src = Avatars.MinIdenticon.getDataUrl("alienHead66.svg"),
                        alt = "Avatars",
                    )
                }
                noui.center.aligned.content {
                    +"Avatars"
                }
            }

            noui.card A {
                href = routes.browserDetect()

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
                href = routes.chartJs()

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
                href = routes.jwtDecode()

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
                href = routes.konva()

                noui.center.aligned.content {
                    renderLogo(
                        src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAAAeCAYAAADnydqVAAACxUlEQVRoQ+2aS1bDMAxFG8aUzcD2YEC3RzdDGROsnroYo89TaiUc4w5bR7Z09XUz7canawtMXWs3lNsNwJ07wQA8AHdugc7Vc0fw/vU4T8xT78+P128fDseZs9ucvj29fK8r12jPcPvRs+WepSzkjLRe2rOWK8nj9Nkf3uaJqXya7nSW+2TXO8au1nOWf64KWAKiGVtTQAScHExSDHFETi7qDOi6Wi/pOc2RLbj0+78DXBrMAwNdi67zAL4Fcp+AhTKSjUr145RKigcGupZbl/eTIk6L3uuZlfKmRXKXgKV6VhqC0jAKjZ5D6zAnUytNnvJkyeFANwcsGcJKM5Zh2MgQvBoBTM2Lt3mzHEJqsLy9B0U80kPE1GChgdEiIh/Eq6jVEM27OaXap1+2QAAvad4swB4nzPtLji05y2ei/yFMIm0iWOlQlxjNUjT/zmUGaYSIAmyNX1YWQhqrUifLoVaNYGkztG5YxrGMW+6vzZRSWq7rs6chyjpaOiCAS3t5dJbO66/BCyIYgWwZx6OsBPhcRlKHbQ2H3lKSKsXuPaVNSwc3YKUcItFLa1YB3KIG7x3KaoCRrlUDrHXTdXZYMh79iGCHzptGcBPAwBVp3scCbEG2Mk4dqVJH7pWDRqUlt5TTLIJzc7D0yg1Jb8gaUg4BrEG2DIhcTJD8VnJq8JbcUMBnwwl1LqLr5ZQNBwzUceRPAtRR/hxgb3TcMtZsAdhK8Uj0ttY5vAbXHuuZ4VorGx3BkYDri4wllyc3pWhxxqxulXoHrF3JIhGMTgUDcPGigaeL9pYRLgVKTozUX/Zmjrl63Q5wGvLm6XItfnl/o3xbozwY3RmfR27wLy/KEteZkpGNjhPR60jHUjc6KnpPzDailwuT8tyrA4422pDf1gLuObjt9kNatAUG4GgLbyx/AN4YQPT2A3C0hTeWPwBvDCB6+y9quqY9QKQ1sAAAAABJRU5ErkJggg==",
                        alt = "Konva Logo",
                    )
                }
                noui.center.aligned.content {
                    +"Konva"
                }
            }

            noui.card A {
                href = routes.marked()

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
                href = routes.pdfjs.index()

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
                href = routes.prismjs()

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
                href = routes.signaturePad()

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
        ui.four.doubling.cards {
            noui.card A {
                href = routes.core.scriptLoader()

                noui.center.aligned.content {
                    +"Script Loader"
                }
            }
        }
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
