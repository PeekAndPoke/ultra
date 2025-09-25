package de.peekandpoke.kraft.common

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.ModalsStage(
    modals: ModalsManager,
) = comp(
    ModalsStage.Props(modals = modals)
) {
    ModalsStage(it)
}

class ModalsStage(ctx: Ctx<Props>) : Component<ModalsStage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val modals: ModalsManager,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val current: List<ModalsManager.Handle> by subscribingTo(props.modals)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div(classes = "modal-stage") {
            current.forEach {
                it.view(this, it)
            }
        }
    }
}
