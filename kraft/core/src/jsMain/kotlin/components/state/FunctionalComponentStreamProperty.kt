package de.peekandpoke.kraft.components.state

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.streams.Stream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

class FunctionalComponentStreamProperty<T>(
    val component: Component<*>,
    val stream: Stream<T>,
    type: KType,
) : ReadOnlyProperty<Any?, T> {

    private val typeStr: String = type.toString()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {

        component.internalData.getOrPut(property.getFullName()) {
            with(component) {
                var first = true
                stream.subscribe {
                    // The first value will be received right away, but we do not want to trigger a re-draw.
                    if (first) {
                        first = false
                    } else {
                        // redraw the component
                        triggerRedraw()
                    }
                }
            }
        }

        return stream()
    }

    private fun KProperty<*>.getFullName() = "sub::$name::$typeStr"
}
