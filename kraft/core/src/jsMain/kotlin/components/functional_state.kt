package io.peekandpoke.kraft.components

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.components.state.FunctionalComponentStateProperty
import io.peekandpoke.kraft.components.state.FunctionalComponentStreamProperty
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.Stream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.typeOf

@KraftDsl
inline fun <reified P> VDom.value(noinline initial: () -> P): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component, initial, typeOf<P>())
}

@KraftDsl
inline fun <reified P> VDom.value(
    noinline initial: () -> P,
    noinline onChange: (P) -> Unit,
): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component, initial, typeOf<P>()).onChange(onChange)
}

@KraftDsl
inline fun <reified P> VDom.subscribingTo(stream: Stream<P>): ReadOnlyProperty<Any?, P> {
    return FunctionalComponentStreamProperty(component, stream, typeOf<P>())
}
