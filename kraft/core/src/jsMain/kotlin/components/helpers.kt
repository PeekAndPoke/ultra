package io.peekandpoke.kraft.components

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.vdom.VDomTagConsumer
import kotlinx.html.FlowContent
import kotlinx.html.Tag

/**
 * A component that can be automatically mounted to the DOM
 */
interface AutoMountedUi {
    /** The priority of the component. Higher values are mounted first */
    val autoMountPriority: Int get() = 0

    /** Mounts the component to the given element */
    fun autoMount(element: FlowContent)
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
