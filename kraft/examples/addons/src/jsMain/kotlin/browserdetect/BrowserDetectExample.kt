package de.peekandpoke.kraft.examples.jsaddons.browserdetect

import de.peekandpoke.kraft.addons.browserdetect.BrowserDetect
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.Overflow
import kotlinx.css.overflow
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.BrowserDetectExample() = comp {
    BrowserDetectExample(it)
}

class BrowserDetectExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val detect = BrowserDetect.forCurrentBrowser()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Browser Detect" }

            ui.dividing.header { +"Detected" }

            ui.striped.table Table {
                tbody {
                    tr {
                        val browser = detect.getBrowser()

                        td {
                            +"Browser"
                        }
                        td {
                            +"${browser.name} - ${browser.version}"
                        }
                    }
                    tr {
                        val os = detect.getOs()

                        td {
                            +"OS"
                        }
                        td {
                            +"${os.name} - ${os.version} - ${os.versionName}"
                        }
                    }
                    tr {
                        val platform = detect.getPlatform()

                        td {
                            +"Platform"
                        }
                        td {
                            +platform.type
                        }
                    }
                    tr {
                        val engine = detect.getEngine()

                        td {
                            +"Engine"
                        }
                        td {
                            +"${engine.name} - ${engine.version}"
                        }
                    }
                    tr {
                        td {
                            +"Supports PDF"
                        }
                        td {
                            if (detect.supportsPdf()) {
                                +"YES"
                            } else {
                                +"NO"
                            }
                        }
                    }
                    tr {
                        td {
                            +"Supported mime-types"
                        }
                        td {
                            +detect.getMimeTypes().joinToString(", ") { it.type }
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
