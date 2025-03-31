package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.utils.launch
import kotlinx.coroutines.delay

interface ComponentRef<C : Component<*>> {

    /**
     * Gets the tracked component or null.
     */
    fun getOrNull(): C?

    /**
     * Gets the tracked component.
     *
     * Will throw a [IllegalStateException] if no component is tracked (yet).
     */
    fun get(): C = getOrNull() ?: throw IllegalStateException("Component is not tracked.")

    /**
     * Shortcut for [getOrNull]
     */
    operator fun invoke(): C? = getOrNull()

    /**
     * Execute code on the tracked component.
     *
     * Gets the component and if the component is not null executes the [block] with the tracked
     * component as parameter.
     *
     * Will return the result of [block] or null if no component is tracked.
     */
    operator fun <R> invoke(block: (C) -> R): R? {
        return getOrNull()?.let {
            block(it)
        }
    }

    /**
     * Tracks a component with the giver [tracker].
     */
    fun track(tracker: Tracker<C>): ComponentRef<C> = apply {
        tracker.track(this)
    }

    /**
     * Tracker implementation of [ComponentRef]
     */
    class Tracker<C : Component<*>> : ComponentRef<C> {

        private var previousRef: ComponentRef<C>? = null
        private var currentRef: ComponentRef<C>? = null

        private var alreadyTracked: Boolean = false

        override fun getOrNull(): C? {
            return currentRef?.getOrNull() ?: previousRef?.getOrNull()
        }

        internal fun track(ref: ComponentRef<C>) {
            previousRef = currentRef
            currentRef = ref

            if (!alreadyTracked) {
                waitForComponent()
            }
        }

        private fun waitForComponent() {
            val comp = getOrNull()

            if (comp == null) {
                launch {
                    delay(10)
                    waitForComponent()
                }
            } else {
                alreadyTracked = true
                comp.parent?.triggerRedraw()
            }
        }
    }

    /**
     * Null implementation of [ComponentRef].
     *
     * This implementation will never track a component.
     */
    class Null<C : Component<*>> : ComponentRef<C> {
        override fun getOrNull(): C? {
            return null
        }
    }
}
