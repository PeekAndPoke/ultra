package de.peekandpoke.kraft.components.state

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.utils.launch
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ComponentStateProperty<T>(
    private val component: Component<*>,
    private val initialValue: T,
    private val onChange: ((T) -> Unit)? = null,
) : ObservableProperty<T>(initialValue) {

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        if (oldValue != newValue) {
            // notify about the change and trigger redraw
            onChange?.invoke(newValue)
            // force the component to redraw
            component.triggerRedraw()
        }
    }

    infix fun setupBy(block: suspend () -> Unit): ComponentStateProperty<T> = apply {
        launch { block() }
    }

    infix fun onChange(onChange: (T) -> Unit): ComponentStateProperty<T> =
        ComponentStateProperty(component = component, initialValue = initialValue, onChange = onChange)
}
