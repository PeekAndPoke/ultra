package io.peekandpoke.kraft.components.state

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.ultra.streams.StreamSource
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ComponentStreamProperty<T>(
    val component: Component<*>,
    val stream: StreamSource<T>,
) : ObservableProperty<T>(stream()) {

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {

        if (oldValue != newValue) {
            // notify about the change and trigger redraw
            stream(newValue)
            // force the component to redraw
            component.triggerRedraw()
        }
    }
}
