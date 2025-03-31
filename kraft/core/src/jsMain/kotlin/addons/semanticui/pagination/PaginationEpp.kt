package de.peekandpoke.kraft.addons.semanticui.pagination

import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.PaginationEpp(
    epp: Int,
    options: Set<Int> = setOf(10, 20, 50, 100, 500),
    onChange: (Int) -> Unit,
) = comp(
    PaginationEpp.Props(
        epp = epp,
        options = options,
        onChange = onChange
    )
) {
    PaginationEpp(it)
}

class PaginationEpp(ctx: Ctx<Props>) : Component<PaginationEpp.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val epp: Int,
        val options: Set<Int>,
        val onChange: (Int) -> Unit,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        SelectField(props.epp, props.onChange) {
            props.options.forEach {
                option(it) { +"$it / page" }
            }
        }
    }
}
