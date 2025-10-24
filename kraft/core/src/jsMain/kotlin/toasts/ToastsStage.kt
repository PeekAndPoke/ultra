package de.peekandpoke.kraft.toasts

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.classes
import kotlinx.html.div

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
        val cssClasses: List<String> = listOf("ui", "toast-container", "top", "right"),
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val current by subscribingTo(props.toasts)

    private val options get() = props.toasts.settings.stageOptions

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        if (current.isNotEmpty()) {
            div {
                classes += options.cssClasses

                current.forEach { toast ->
                    toast.view(this, toast)
                }
            }
        }
    }
}
