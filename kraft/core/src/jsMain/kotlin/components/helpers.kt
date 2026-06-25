package io.peekandpoke.kraft.components

import io.peekandpoke.kraft.vdom.VDomTagConsumer
import kotlinx.html.Tag

/**
 * Adds a child component to the current tag
 */
inline fun <P, reified C : Component<P>> Tag.comp(props: P, noinline component: (Ctx<P>) -> C): ComponentRef<C> {
    return (this.consumer as VDomTagConsumer).onComponent(props, component, C::class)
}

/**
 * Adds a parameterless child component to the current tag
 */
inline fun <reified C : Component<Any?>> Tag.comp(noinline component: (NoProps) -> C): ComponentRef<C> {
    return comp(null, component)
}
