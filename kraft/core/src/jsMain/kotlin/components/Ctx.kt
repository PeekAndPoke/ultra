package io.peekandpoke.kraft.components

import io.peekandpoke.kraft.vdom.VDomEngine

/** Type alias for component contexts that carry no props. */
typealias NoProps = Ctx<Any?>

/**
 * Context passed to a [Component] when it is created or updated.
 *
 * @param engine The VDom engine used for rendering.
 * @param parent The parent component, or null for root components.
 * @param props  The props passed to the component.
 */
data class Ctx<out PROPS>(
    val engine: VDomEngine,
    val parent: Component<*>?,
    val props: PROPS,
) {
    companion object {
        /** Creates a root context with no parent and no props. */
        fun of(engine: VDomEngine) = Ctx(engine, null, null)
    }
}
