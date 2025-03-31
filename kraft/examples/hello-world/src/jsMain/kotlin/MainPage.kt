package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.addons.forms.formController
import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.semanticui.forms.UiDateTimeField
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectFieldComponent
import de.peekandpoke.kraft.addons.semanticui.modals.OkCancelModal
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import kotlinx.browser.window
import kotlinx.coroutines.flow.flowOf
import kotlinx.css.vw
import kotlinx.css.width
import kotlinx.html.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.time.Duration.Companion.days

@Suppress("FunctionName")
fun Tag.MainPage() = comp {
    MainPage(it)
}

class MainPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val formCtrl = formController()

    private var select by value("")

    //    private var datetime by value(Kronos.systemUtc.zonedDateTimeNow(MpTimezone.UTC))
    private var datetime by value(MpZonedDateTime.Genesis.plus(10000.days))

    private var textAreaContent by value("")

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.container {
            ui.basic.form.segment {
                h1 { +"Hello World!" }

                ui.basic.button {
                    onClick {
                        formCtrl.validate()
                    }
                    +"Validate form"
                }

                h2 { +"First Counter" }

                CounterComponent(0)

                h2 { +"Another Counter" }

                CounterComponent(10)

                h2 { +"A ticker" }

                TickerComponent(100)

                h2 { +"Component storing data in local storage" }

                LocalStorageComponent("INITIAL")

                h2 { +"Component with a DataLoader" }

                DataLoaderComponent(100)

                ui.divider()

                h2 { +"Date Fields" }

                ui.three.fields {
                    UiDateTimeField(::datetime) {
                        label("DateTime")
                    }
                    ui.field {
                        ui.basic.fluid.button {
                            onClick { datetime = MpZonedDateTime.Genesis.plus(365.days * 9200) }
                            +"Genesis"
                        }
                    }
                    ui.field {
                        ui.basic.fluid.button {
                            onClick { datetime = MpZonedDateTime.Doomsday }
                            +"Doomsday"
                        }
                    }
                }

                ui.divider()

                h2 { +"Select Field" }

                ui.three.fields {

                    SelectField(value = select, { select = it }) {
                        label("Static Options")

                        option("One", "One")
                        option("Two", "Two")
                        option("Three", "Three")
                        option("Four", "Four")
                    }

                    SelectField(value = select, { select = it }) {
                        label("Searchable")

                        searchableBy { search ->
                            listOf("One", "Two", "Three", "Four")
                                .filter { search.isBlank() || it.startsWith(search, true) }
                                .map {
                                    SelectFieldComponent.Option(it, it)
                                }
                        }
                    }

                    SelectField(value = select, { select = it }) {
                        label("Auto-Suggest")

                        autoSuggest { search ->
                            flowOf(
                                listOf("One", "Two", "Three", "Four")
                                    .filter { search.isBlank() || it.startsWith(search, true) }
                                    .map {
                                        SelectFieldComponent.Option(it, it)
                                    }
                            )
                        }
                    }
                }

                ui.divider()

                h2 { +"Modal Popups" }

                ui.blue.button {
                    onClick {
                        modals.show { handle ->
                            OkCancelModal {
                                mini(
                                    handle = handle,
                                    header = { ui.header { +"Modal Dialog" } },
                                    content = {
                                        ui.content {
                                            ui.list {
                                                noui.item {
                                                    +"Click any button ..."
                                                }
                                                noui.item {
                                                    +"Or navigate back to close the popup."
                                                }
                                                noui.item {
                                                    a {
                                                        onClick {
                                                            router.navToUri("404")
                                                        }
                                                        +"Or nav away"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                ) {
                                    handle.close()
                                }
                            }
                        }
                    }

                    +"Show Modal Popup"
                }

                ui.divider()

                h2 { +"Left Click | Aux Click | Right Click" }

                ui.card {
                    fun evt2str(evt: PointerEvent) = """
                        MouseEvent:
                            screenX: ${evt.screenX}
                            screenY: ${evt.screenY}
                            clientX: ${evt.clientX}
                            clientY: ${evt.clientY}
                            ctrlKey: ${evt.ctrlKey}
                            shiftKey: ${evt.shiftKey}
                            altKey: ${evt.altKey}
                            metaKey: ${evt.metaKey}
                            button: ${evt.button}
                            buttons: ${evt.buttons}
                            relatedTarget: ${evt.relatedTarget}
                            region: ${evt.region}?
                            pageX: ${evt.pageX}
                            pageY: ${evt.pageY}
                            x: ${evt.x}
                            y: ${evt.y}
                            offsetX: ${evt.offsetX}
                            offsetY: ${evt.offsetY}
                        PointerEvent:
                            pointerId: ${evt.pointerId}
                            width: ${evt.width}
                            height: ${evt.height}
                            pressure: ${evt.pressure}
                            tangentialPressure: ${evt.tangentialPressure}
                            tiltX: ${evt.tiltX}
                            tiltY: ${evt.tiltY}
                            twist: ${evt.twist}
                            pointerType: ${evt.pointerType}
                            isPrimary: ${evt.isPrimary}                        
                    """.trimIndent()

                    onClick {
                        val str = "Left Click:\n\n${evt2str(it)}"
                        console.log(str)
                    }

                    onAuxClick {
                        val str = "Middle Click:\n\n${evt2str(it)}"
                        console.log(str)
                    }

                    onContextMenu {
                        val str = "Right Click:\n\n${evt2str(it)}"
                        console.log(str)
                    }

                    noui.content {
                        +"Click me -> Check console output"
                    }
                }

                ui.divider()

                h2 { +"OnContextMenu" }

                ui.three.cards {
                    noui.card {
                        noui.content {
                            onContextMenuStoppingEvent { evt ->
                                popups.showContextMenu(evt, PopupsManager.Positioning.BottomLeft) {
                                    ui.basic.vertical.menu {
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                    }
                                }
                            }
                            +"Right click: Context Bottom Left"
                        }
                    }

                    noui.card {
                        noui.content {
                            onContextMenuStoppingEvent { evt ->
                                popups.showContextMenu(evt, PopupsManager.Positioning.BottomCenter) {
                                    ui.basic.vertical.menu {
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                    }
                                }
                            }
                            +"Right click: Context Bottom Center"
                        }
                    }

                    noui.card {
                        noui.content {
                            onContextMenuStoppingEvent { evt ->
                                popups.showContextMenu(evt, PopupsManager.Positioning.BottomRight) {
                                    ui.basic.vertical.menu {
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                    }
                                }
                            }
                            +"Right click: Context Bottom Right"
                        }
                    }
                }

                ui.two.cards {
                    noui.card {
                        noui.content {
                            onContextMenuStoppingEvent { evt ->
                                popups.showContextMenu(evt, PopupsManager.Positioning.BottomRight) {
                                    ui.basic.vertical.menu {
                                        css { width = 70.vw }
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                    }
                                }
                            }
                            +"Right click: Context Bottom Right"
                        }
                    }

                    noui.card {
                        noui.content {
                            onContextMenuStoppingEvent { evt ->
                                popups.showContextMenu(evt, PopupsManager.Positioning.BottomLeft) {
                                    ui.basic.vertical.menu {
                                        css { width = 70.vw }
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                    }
                                }
                            }
                            +"Right click: Context Bottom Left"
                        }
                    }
                }

                ui.divider {}

                h2 { +"Popups" }

                ui.horizontal.list {
                    noui.item {
                        ui.basic.label {
                            onClick {
                                popups.showContextMenu(it, PopupsManager.Positioning.BottomLeft) {
                                    ui.horizontal.menu {
                                        css {
                                            width = 50.vw
                                        }
                                        noui.item { a { href = "#"; +"Menu 1" } }
                                        noui.item { a { href = "#"; +"Menu 2" } }
                                        noui.item { a { href = "#"; +"Menu 3" } }
                                        noui.item { a { href = "#"; +"Menu 4" } }
                                        noui.item { a { href = "#"; +"Menu 5" } }
                                    }
                                }
                            }
                            +"Bottom Left Popup"
                        }
                    }

                    noui.item {
                        ui.basic.label {
                            onClick {
                                popups.showContextMenu(it, PopupsManager.Positioning.BottomRight) {
                                    ui.basic.horizontal.menu {
                                        css {
                                            width = 50.vw
                                        }
                                        noui.item A { href = "#"; +"Menu 1" }
                                        noui.item A { href = "#"; +"Menu 2" }
                                        noui.item A { href = "#"; +"Menu 3" }
                                        noui.item A { href = "#"; +"Menu 4" }
                                        noui.item A { href = "#"; +"Menu 5" }
                                    }
                                }
                            }
                            +"Bottom Right Popup"
                        }
                    }
                }

                ui.divider {}

                h2 { +"Textarea customization" }

                UiTextArea(textAreaContent, { textAreaContent = it }) {
                    label("Customized TextArea")

                    customize {
                        onKeyDown { evt: KeyboardEvent ->
                            if (evt.key == "Enter" && !evt.shiftKey) {
                                evt.preventDefault()
                                evt.stopPropagation()

                                window.alert("Enter pressed")
                            }
                        }
                    }
                }

                ui.divider {}

                h2 { +"Unsafe Content" }

                style {
                    unsafe {
                        +"""
                            .red-text {
                              color: #FF0000;
                            }
                        """.trimIndent()
                    }
                }

                div("red-text") {
                    +"This text should be red"
                }

                ui.divider {}

                ui.two.column.grid {
                    ui.column {
                        ui.segment {
                            ui.header H2 { +"Playground" }
                        }
                    }
                    ui.column {
                        ui.red.button { +"red" }
                        ui.orange.button { +"orange" }
                        ui.yellow.button { +"yellow" }
                        ui.olive.button { +"olive" }
                        ui.green.button { +"green" }
                        ui.teal.button { +"teal" }
                        ui.blue.button { +"blue" }
                        ui.violet.button { +"violet" }
                        ui.purple.button { +"purple" }
                        ui.pink.button { +"pink" }
                        ui.brown.button { +"brown" }
                        ui.black.button { +"black" }
                        ui.white.button { +"white" }
                    }
                }
            }
        }
    }
}
