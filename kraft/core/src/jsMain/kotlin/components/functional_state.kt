package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.components.state.FunctionalComponentStateProperty
import de.peekandpoke.kraft.components.state.FunctionalComponentStreamProperty
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.vdom.VDom
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.typeOf

inline fun <reified P> VDom.value(noinline initial: () -> P): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component!!, initial, typeOf<P>())
}

inline fun <reified P> VDom.value(
    noinline initial: () -> P,
    noinline onChange: (P) -> Unit,
): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component!!, initial, typeOf<P>()).onChange(onChange)
}

inline fun <reified P> VDom.subscribingTo(stream: Stream<P>): ReadOnlyProperty<Any?, P> {
    return FunctionalComponentStreamProperty(component!!, stream, typeOf<P>())
}
