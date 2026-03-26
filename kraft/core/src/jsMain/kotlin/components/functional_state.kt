package io.peekandpoke.kraft.components

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.components.state.FunctionalComponentStateProperty
import io.peekandpoke.kraft.components.state.FunctionalComponentStreamProperty
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.Stream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.typeOf

/** Creates a mutable state property for use in functional components. Triggers redraw on change. */
@KraftDsl
inline fun <reified P> VDom.value(noinline initial: () -> P): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component, initial, typeOf<P>())
}

/** Creates a mutable state property with an [onChange] callback for use in functional components. */
@KraftDsl
inline fun <reified P> VDom.value(
    noinline initial: () -> P,
    noinline onChange: (P) -> Unit,
): FunctionalComponentStateProperty<P> {
    return FunctionalComponentStateProperty(component, initial, typeOf<P>()).onChange(onChange)
}

/** Creates a read-only property that stays in sync with the given [stream]. Triggers redraw on new values. */
@KraftDsl
inline fun <reified P> VDom.subscribingTo(stream: Stream<P>): ReadOnlyProperty<Any?, P> {
    return FunctionalComponentStreamProperty(component, stream, typeOf<P>())
}
