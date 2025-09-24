package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.ops.persistInLocalStorage
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.serialization.Serializable

@Suppress("FunctionName")
fun Tag.LocalStorageComponent(
    start: String,
) = comp(
    LocalStorageComponent.Props(start = start)
) {
    LocalStorageComponent(it)
}

class LocalStorageComponent(ctx: Ctx<Props>) : Component<LocalStorageComponent.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val start: String,
    )

    @Serializable
    data class Content(
        val text: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val persistent: StreamSource<Content> = StreamSource(Content(props.start))
        .persistInLocalStorage("string-key", Content.serializer())

    private val value by subscribingTo(persistent)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div {
            div { +"Value: $value" }

            UiInputField(value.text, {
                console.log("form input changed", it)
                persistent.invoke(
                    value.copy(text = it)
                )
            })
        }
    }
}
