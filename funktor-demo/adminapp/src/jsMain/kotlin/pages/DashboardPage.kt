package io.peekandpoke.funktor.demo.adminapp.pages

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager.Companion.modals
import io.peekandpoke.kraft.popups.PopupsManager
import io.peekandpoke.kraft.popups.PopupsManager.Companion.popups
import io.peekandpoke.kraft.semanticui.modals.OkCancelModal
import io.peekandpoke.kraft.semanticui.popups.topCenter
import io.peekandpoke.kraft.semanticui.popups.topLeft
import io.peekandpoke.kraft.semanticui.popups.topRight
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.fixture.LoremIpsum
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onContextMenu
import io.peekandpoke.ultra.model.Message
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

//    private val auth by subscribingTo(State.auth)
//    private val user get() = auth.user!!

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Dashboard" }
        }

        renderDemoContent()
    }

    private fun FlowContent.renderDemoContent() {
        ui.segment {
            ui.header H2 { +"Demo: Toasts" }

            ui.button {
                onClick {
                    toasts.append(
                        Message.Type.entries.random(),
                        LoremIpsum.words(5, 4)
                    )
                }
                +"Show random toast"
            }

            ui.divider()

            ui.header H2 { +"Demo: Modal Dialogs" }

            ui.button {
                onClick {
                    modals.show { handle ->
                        OkCancelModal {
                            mini(
                                handle = handle,
                                header = {
                                    ui.header { +"Header" }
                                },
                                content = {
                                    ui.content { +"Some content" }
                                }
                            ) {
                                window.alert("Exited with: ${it.name}")
                            }
                        }
                    }
                }
                +"Show random modal"
            }

            ui.header H2 { +"Demo: Popups" }

            ui.four.column.doubling.stackable.grid {
                noui.column {
                    ui.fluid.button {
                        onClick { evt ->
                            popups.showContextMenu(evt, PopupsManager.Positioning.BottomLeft) {
                                ui.vertical.menu {
                                    noui.item {
                                        ui.header { +"Context Menu" }
                                    }
                                    noui.item {
                                        +"Some content"
                                    }
                                }
                            }
                        }

                        +"Context menu bottom-left"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        onClick { evt ->
                            popups.showContextMenu(evt, PopupsManager.Positioning.BottomCenter) {
                                ui.vertical.menu {
                                    noui.item {
                                        ui.header { +"Context Menu" }
                                    }
                                    noui.item {
                                        +"Some content"
                                    }
                                }
                            }
                        }

                        +"Context menu bottom-center"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        onClick { evt ->
                            popups.showContextMenu(evt, PopupsManager.Positioning.BottomRight) {
                                ui.vertical.menu {
                                    noui.item {
                                        ui.header { +"Context Menu" }
                                    }
                                    noui.item {
                                        +"Some content"
                                    }
                                }
                            }
                        }

                        +"Context menu bottom-right"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        onContextMenu { evt ->
                            evt.preventDefault()

                            popups.showContextMenu(evt) {
                                ui.vertical.menu {
                                    noui.item {
                                        ui.header { +"Context Menu" }
                                    }
                                    noui.item {
                                        +"Some content"
                                    }
                                }
                            }
                        }

                        +"Context menu - Right click"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        popups.showHoverPopup.topLeft(tag = this) {
                            +"Top Left"
                        }

                        +"Hover: top-left"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        popups.showHoverPopup.topCenter(tag = this) {
                            +"Top Center"
                        }

                        +"Hover: top-center"
                    }
                }

                noui.column {
                    ui.fluid.button {
                        popups.showHoverPopup.topRight(tag = this) {
                            +"Top Right"
                        }

                        +"Hover: top-right"
                    }
                }
            }
        }
    }
}
