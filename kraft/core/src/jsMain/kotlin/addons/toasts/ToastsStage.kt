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
    toasts: ToastsManager,
    options: ToastsStage.Options = toasts.settings.stageOptions,
) = comp(
    ToastsStage.Props(
        toasts = toasts,
        options = options,
    )
) {
    ToastsStage(it)
}

class ToastsStage(ctx: Ctx<Props>) : Component<ToastsStage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val toasts: ToastsManager,
        val options: Options,
    )

    data class Options(
        val positioning: SemanticFn = { top.right },
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val current by subscribingTo(props.toasts)

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
