package io.peekandpoke.kraft.modals

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

/** Renders the [ModalsStage] component that displays all currently open modals. */
@Suppress("FunctionName")
fun Tag.ModalsStage(
    modals: ModalsManager,
) = comp(
    ModalsStage.Props(modals = modals)
) {
    ModalsStage(it)
}

/** Component that subscribes to a [ModalsManager] and renders all open modal dialogs. */
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
