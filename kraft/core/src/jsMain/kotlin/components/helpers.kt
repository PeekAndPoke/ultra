package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.KraftDsl
import de.peekandpoke.kraft.vdom.VDomTagConsumer
import kotlinx.html.FlowContent
import kotlinx.html.Tag

interface AutoMountedUi {
    val priority: Int get() = 0

    fun mount(flow: FlowContent)
}

/**
 * Adds a child component to the current tag
 */
@KraftDsl
inline fun <P, reified C : Component<P>> Tag.comp(props: P, noinline component: (Ctx<P>) -> C): ComponentRef<C> {
    return (this.consumer as VDomTagConsumer).onComponent(props, component, C::class)
}

/**
 * Adds a parameterless child component to the current tag
 */
@KraftDsl
inline fun <reified C : Component<Any?>> Tag.comp(noinline component: (NoProps) -> C): ComponentRef<C> {
    return comp(null, component)
}
