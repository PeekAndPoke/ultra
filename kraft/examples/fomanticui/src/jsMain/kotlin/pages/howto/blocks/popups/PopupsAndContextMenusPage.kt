package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.popups

import de.peekandpoke.kraft.addons.popups.PopupsManager.Positioning
import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onMouseEnter
import de.peekandpoke.kraft.examples.fomanticui.kraft
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.PopupsAndContextMenusPage() = comp {
    PopupsAndContextMenusPage(it)
}

class PopupsAndContextMenusPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        PageTitle("How To | Popups")

        ui.basic.padded.segment {
            ui.header H2 { +"Popups & Context Menus" }

            ui.header H3 { +"Context Menus" }

            ui.four.column.grid {
                noui.column {
                    ui.fluid.basic.button {
                        onMouseEnter { evt ->
                            kraft.popups.showContextMenu(evt) {
                                ui.vertical.menu {
                                    noui.item A { +"Item 1" }
                                    noui.item A { +"Item 2" }
                                }
                            }
                        }

                        +"Hover me -> default"
                    }
                }

                noui.column {
                    ui.fluid.basic.button {
                        onMouseEnter { evt ->
                            kraft.popups.showContextMenu(evt, Positioning.BottomLeft) {
                                ui.vertical.menu {
                                    noui.item A { +"Item 1" }
                                    noui.item A { +"Item 2" }
                                }
                            }
                        }

                        +"Hover me -> bottom left"
                    }
                }

                noui.column {
                    ui.fluid.basic.button {
                        onMouseEnter { evt ->
                            kraft.popups.showContextMenu(evt, Positioning.BottomRight) {
                                ui.vertical.menu {
                                    noui.item A { +"Item 1" }
                                    noui.item A { +"Item 2" }
                                }
                            }
                        }

                        +"Hover me -> bottom right"
                    }
                }

                noui.column {
                    ui.fluid.basic.button {
                        onMouseEnter { evt ->
                            kraft.popups.showContextMenu(evt, Positioning.BottomCenter) {
                                ui.vertical.menu {
                                    noui.item A { +"Item 1" }
                                    noui.item A { +"Item 2" }
                                }
                            }
                        }

                        +"Hover me -> bottom Center"
                    }
                }
            }

        }
    }
}
