package de.peekandpoke.kraft.addons.routing

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.JoinedPageTitle(parts: () -> Iterable<String?>) {
    JoinedPageTitle(glue = " | ", parts = parts)
}

@Suppress("FunctionName")
fun Tag.JoinedPageTitle(glue: String, parts: () -> Iterable<String?>) {
    PageTitle(provider = { parts().filterNotNull().filter { it.isNotBlank() }.joinToString(glue) })
}

@Suppress("FunctionName")
fun Tag.PageTitle(title: String) {
    PageTitle { title }
}

@Suppress("FunctionName")
fun Tag.PageTitle(
    provider: () -> String,
) = comp(
    PageTitle.Props(provider = provider)
) {
    PageTitle(it)
}

class PageTitle(ctx: Ctx<Props>) : Component<PageTitle.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val provider: () -> String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        window.document.title = props.provider()
    }
}
