package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.ops.persistInLocalStorage
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
