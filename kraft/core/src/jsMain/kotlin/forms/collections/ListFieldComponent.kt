package io.peekandpoke.kraft.forms.collections

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.modifyAt
import io.peekandpoke.kraft.utils.removeAt
import io.peekandpoke.kraft.utils.swap
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.common.addAt
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun <T> Tag.ListField(
    items: List<T>,
    onChange: (List<T>) -> Unit,
    customize: ListFieldComponent.PropsBuilder<T>.() -> Unit,
) = comp(
    ListFieldComponent.PropsBuilder(items, onChange).apply(customize).build()
) {
    ListFieldComponent(it)
}

class ListFieldComponent<T>(ctx: Ctx<Props<T>>) :
    Component<ListFieldComponent.Props<T>>(ctx) {

    data class Props<T>(
        val items: List<T>,
        val onChange: (List<T>) -> Unit,
        val renderItem: FlowContent.(ItemCtx<T>) -> Unit,
        val renderAdd: FlowContent.(AddCtx<T>) -> Unit,
    )

    class PropsBuilder<T>(
        val items: List<T>,
        val onChange: (List<T>) -> Unit,
    ) {
        private var renderItem: FlowContent.(ItemCtx<T>) -> Unit = {}
        private var renderAdd: FlowContent.(AddCtx<T>) -> Unit = {}

        fun build() = Props(items, onChange, renderItem, renderAdd)

        fun renderItem(block: FlowContent.(ItemCtx<T>) -> Unit) {
            renderItem = block
        }

        fun renderAdd(block: FlowContent.(AddCtx<T>) -> Unit) {
            renderAdd = block
        }
    }

    data class ItemCtx<T>(
        val idx: Int,
        val item: T,
        val all: List<T>,
        val modify: (T) -> Unit,
        val swapWith: (idx: Int) -> Unit,
        val remove: () -> Unit,
        val copy: (T) -> Unit,
        private val keySuffix: String,
    ) {
        fun domKey(): String {
            val cls = (item ?: Unit)::class

            return "item-$idx-${cls::simpleName}-${cls.hashCode()}-$keySuffix"
        }

        fun <V> modifier(block: T.(value: V) -> T): (V) -> Unit = { value ->
            modify(item.block(value))
        }
    }

    data class AddCtx<T>(
        val add: (T) -> Unit,
    ) {
        fun domKey() = "add-item"
    }

    private var reorderCounter by value(0)

    ////  IMPL  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        props.apply {

            items.forEachIndexed { idx, item ->
                val ctx = ItemCtx(
                    idx = idx,
                    item = item,
                    all = props.items,
                    modify = { new -> onChange(items.modifyAt(idx) { new }) },
                    swapWith = { other -> onChange(items.swap(idx, other)) },
                    remove = { onChange(items.removeAt(idx)) },
                    copy = { copied -> onChange(items.addAt(idx + 1, copied)) },
                    keySuffix = "$reorderCounter"
                )

                props.renderItem(this@render, ctx)
            }

            val ctx = AddCtx { new: T -> onChange(items.plus(new)) }

            props.renderAdd(this@render, ctx)
        }
    }
}
