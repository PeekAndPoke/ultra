package de.peekandpoke.kraft.addons.toasts

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.SemanticFn
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.ToastsStage(
    flashMessages: ToastsManager,
    options: ToastsStage.Options = ToastsStage.Options(),
) = comp(
    ToastsStage.Props(
        flashMessages = flashMessages,
        options = options,
    )
) {
    ToastsStage(it)
}

class ToastsStage(ctx: Ctx<Props>) : Component<ToastsStage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val flashMessages: ToastsManager,
        val options: Options,
    )

    data class Options(
        val positioning: SemanticFn = { bottom.right },
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val current by subscribingTo(props.flashMessages)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        if (current.isNotEmpty()) {
            val positioning = props.options.positioning

            ui.positioning().toastContainer.with("flash-messages") {
                current.forEach { toast ->
                    toast.view(this, toast)
                }
            }
        }
    }
}
