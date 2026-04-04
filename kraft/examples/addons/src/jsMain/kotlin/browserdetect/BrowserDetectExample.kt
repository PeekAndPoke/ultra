package io.peekandpoke.kraft.examples.jsaddons.browserdetect

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.browserdetect.BrowserDetect
import io.peekandpoke.kraft.addons.browserdetect.BrowserDetectAddon
import io.peekandpoke.kraft.addons.browserdetect.browserDetect
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Overflow
import kotlinx.css.overflow
import kotlinx.html.Tag
import kotlinx.html.pre
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.BrowserDetectExample() = comp {
    BrowserDetectExample(it)
}

class BrowserDetectExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // <CodeBlock subscribing>
    private val browserDetectAddon: BrowserDetectAddon? by subscribingTo(addons.browserDetect)
    // </CodeBlock>

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val addon = browserDetectAddon

        ui.segment {
            ui.header H2 { +"Browser Detect (via AddonRegistry)" }

            if (addon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading browser detect addon..."
                    }
                }
                return@segment
            }

            ui.dividing.header { +"Subscribing to the addon" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.browserdetect_BrowserDetectExample_kt_subscribing,
            ) {
                ui.label { +"BrowserDetectAddon is loaded via subscribingTo(addons.browserDetect)" }
            }

            ui.dividing.header { +"Usage" }

            // <CodeBlock usage>
            val detect = BrowserDetect.forCurrentBrowser(addon)
            // </CodeBlock>

            HorizontalContentAndCode(
                ExtractedCodeBlocks.browserdetect_BrowserDetectExample_kt_usage,
            ) {
                ui.striped.table Table {
                    tbody {
                        tr {
                            val browser = detect.getBrowser()
                            td { +"Browser" }
                            td { +"${browser.name} - ${browser.version}" }
                        }
                        tr {
                            val os = detect.getOs()
                            td { +"OS" }
                            td { +"${os.name} - ${os.version} - ${os.versionName}" }
                        }
                        tr {
                            val platform = detect.getPlatform()
                            td { +"Platform" }
                            td { +platform.type }
                        }
                        tr {
                            val engine = detect.getEngine()
                            td { +"Engine" }
                            td { +"${engine.name} - ${engine.version}" }
                        }
                        tr {
                            td { +"Supports PDF" }
                            td { +if (detect.supportsPdf()) "YES" else "NO" }
                        }
                        tr {
                            td { +"Supported mime-types" }
                            td { +detect.getMimeTypes().joinToString(", ") { it.type } }
                        }
                    }
                }
            }

            ui.dividing.header { +"Raw result" }

            pre {
                css {
                    overflow = Overflow.auto
                }

                +JSON.stringify(detect.getRawResult(), null, 2)
            }
        }
    }
}
