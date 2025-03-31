package de.peekandpoke.kraft.components.state

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.utils.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

class FunctionalComponentStateProperty<P>(
    val component: Component<*>,
    val initial: () -> P,
    type: KType,
) : ReadWriteProperty<Any?, P> {

    private val lazyInitCallbacks = mutableListOf<suspend () -> Flow<P>>()

    private val onChangeCallbacks = mutableListOf<(P) -> Unit>()

    private val typeStr: String = type.toString()

    override fun getValue(thisRef: Any?, property: KProperty<*>): P {

        @Suppress("UNCHECKED_CAST")
        return component.internalData.getOrPut(property.getPropertyKey()) {
            launch {
                if (lazyInitCallbacks.isNotEmpty()) {
                    // We delay by 1ms to avoid immediate results of the initializers
                    delay(1)

                    lazyInitCallbacks.forEach { init ->
                        init()
                            .catch { e -> console.error(e) }
                            .collect { setValue(thisRef, property, it) }
                    }
                }
            }

            // We return and set the initial value
            initial()
        } as P
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: P) {
        val oldValue = getValue(thisRef, property)

        component.internalData[property.getPropertyKey()] = value

        if (oldValue != value) {

            onChangeCallbacks.forEach {
                it(value)
            }

            component.triggerRedraw()
        }
    }

    /**
     * Adds a callback that will initialize the property when it is accessed for the first time.
     */
    fun initLazy(block: suspend () -> Flow<P>): FunctionalComponentStateProperty<P> = apply {
        lazyInitCallbacks.add(block)
    }

    /**
     * Adds a callback that will be called whenever the value has changed
     */
    fun onChange(block: (P) -> Unit): FunctionalComponentStateProperty<P> = apply {
        onChangeCallbacks.add(block)
    }

    private fun KProperty<*>.getPropertyKey(): String = "value::$name::$typeStr"
}
