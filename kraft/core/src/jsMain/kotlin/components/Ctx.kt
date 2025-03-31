package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.vdom.VDomEngine

typealias NoProps = Ctx<Any?>

data class Ctx<out PROPS>(
    val engine: VDomEngine,
    val parent: Component<*>?,
    val props: PROPS,
) {
    companion object {
        fun of(engine: VDomEngine) = Ctx(engine, null, null)
    }
}
