package de.peekandpoke.kraft.semanticui.menu

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.utils.async
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.RenderFn
import de.peekandpoke.ultra.html.RenderFunc
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.html.onMouseEnter
import de.peekandpoke.ultra.html.onMouseLeave
import de.peekandpoke.ultra.html.onMouseOver
import de.peekandpoke.ultra.semanticui.SemanticTag
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.css.zIndex
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import org.w3c.dom.events.Event

@Suppress("FunctionName")
fun Tag.TextDropdownMenu(
    display: RenderFn,
    items: List<DropdownMenu.Item>,
) = DropdownMenu(
    DropdownMenu.Props(
        content = display,
        style = DropdownMenu.Style.TextMenu,
        items = items,
    )
)

@Suppress("FunctionName")
fun Tag.DropdownMenu(
    props: DropdownMenu.Props,
) = comp(props) {
    DropdownMenu(it)
}

class DropdownMenu(ctx: Ctx<Props>) : Component<DropdownMenu.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    companion object {
        /** Helper to bring the companion into the scope */
        operator fun invoke(block: Companion.() -> Unit) {
            this.block()
        }

        fun Tag.text(build: PropsBuilder.() -> Unit) {
            DropdownMenu(
                PropsBuilder(Style.TextMenu).apply(build).build()
            )
        }
    }

    data class Props(
        val content: RenderFn,
        val style: Style,
        val items: List<Item>,
    )

    class PropsBuilder internal constructor(
        var style: Style,
    ) {
        private var content: RenderFn = { icon.ellipsis_horizontal() }

        var items = mutableListOf<ItemBuilder>()

        internal fun build(): Props = Props(
            content = content,
            style = style,
            items = items.map { it.build() }
        )

        fun render(content: RenderFn) {
            this.content = content
        }

        fun item(builder: ItemBuilder.() -> Unit) {
            items.add(
                ItemBuilder().apply(builder)
            )
        }
    }

    class ItemBuilder internal constructor() {
        private var content: RenderFunc<DIV> = {}
        private var items = mutableListOf<ItemBuilder>()

        internal fun build(): Item = Item(
            content = content,
            items = items.map { it.build() }
        )

        fun render(content: RenderFunc<DIV>) {
            this.content = content
        }

        fun item(builder: ItemBuilder.() -> Unit) {
            items.add(
                ItemBuilder().apply(builder)
            )
        }
    }

    sealed class Style {
        data object TextMenu : Style()
    }

    data class Item(
        val content: RenderFunc<DIV>,
        val items: List<Item> = emptyList(),
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var opened by value(false)
    private var selectedPath by value(emptyList<Int>())

    private var closeOnLeaveJob: Job? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.document.addEventListener("mouseup", onClose)
            }

            onUnmount {
                window.document.removeEventListener("mouseup", onClose)
            }
        }
    }

    private val onClose = { _: Event -> close() }

    private fun close() {
        opened = false
        selectedPath = emptyList()
    }

    override fun VDom.render() {
        ui.applyStyle().then {

            if (opened) {
                onMouseLeave {
                    closeOnLeaveJob?.cancel()
                    closeOnLeaveJob = async {
                        delay(1_000)
                        close()
                    }
                }
                onMouseEnter {
                    closeOnLeaveJob?.cancel()
                }
            }

            ui.dropdown.item {
                onClick { evt ->
                    evt.stopPropagation()
                    opened = true
                }

                props.content(this)

                noui.given(opened) { transition.visible }.menu {
                    css {
                        zIndex = 2000
                    }
                    onClick { evt ->
                        evt.stopPropagation()
                        opened = false
                    }
                    renderItems(emptyList(), props.items)
                }
            }
        }
    }

    private fun FlowContent.renderItems(path: List<Int>, items: List<Item>) {

        items.forEachIndexed { idx, it ->
            val thisPath = path.plus(idx)

            noui.item {
                onMouseOver { evt ->
                    evt.stopPropagation()
                    selectedPath = thisPath
                }

                it.content(this)

                val isSubMenuShown = selectedPath.startsWith(thisPath)

                if (it.items.isNotEmpty()) {
                    noui.transition
                        .given(isSubMenuShown) { visible }
                        .given(!isSubMenuShown) { hidden }
                        .menu {
                            renderItems(thisPath, it.items)
                        }
                }
            }
        }
    }

    private fun SemanticTag.applyStyle(): SemanticTag = when (props.style) {
        is Style.TextMenu -> text
    }

    private fun List<Int>.startsWith(other: List<Int>) = size >= other.size && subList(0, other.size) == other
}
