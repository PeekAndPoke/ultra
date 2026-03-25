@file:Suppress("FunctionName")

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.h4

/** This function is needed to instantiate a component, and maps parameters to the Props */
fun Tag.PropsExampleComponent(name: String) = comp(
    PropsExampleComponent.Props(name = name)
) {
    PropsExampleComponent(it)
}

/** This is the implementation of the Component */
class PropsExampleComponent(ctx: Ctx<Props>) : Component<PropsExampleComponent.Props>(ctx) {
    /** This is the implementation of the Props */
    data class Props(
        val name: String,
    )

    /** This renders the Component */
    override fun VDom.render() {
        h4 { +"Hello ${props.name} !" }
    }
}
// </CodeBlock>
