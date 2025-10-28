package de.peekandpoke.kraft.semanticui.dnd

import org.w3c.dom.events.Event
import kotlin.reflect.KClass

internal object Dnd {

    private data class CurrentDrag<PAYLOAD : Any>(
        val payload: PAYLOAD,
        val possibleTargets: List<DndDropTargetComponent<PAYLOAD>>,
    ) {
        private inner class Ctx(val target: DndDropTargetComponent<PAYLOAD>) {

            val onMouseOver: (Event) -> Unit = {
                it.stopPropagation()
                currentDropTarget = target
                target.onMouseOver()
            }
            val onMouseOut: (Event) -> Unit = {
                it.stopPropagation()
                currentDropTarget = null
                target.onMouseOut()
            }
        }

        /**
         * A list containing an instance of Ctx for each possible target.
         */
        private var contexts: List<Ctx>? = null

        /**
         * The drop target currently hovered or null if none is hovered.
         */
        private var currentDropTarget: DndDropTargetComponent<PAYLOAD>? = null

        fun start() {

            cleanup()

            contexts = possibleTargets.map { Ctx(target = it) }

            contexts?.forEach {
                // add mouse over event listeners
                it.target.parent?.dom?.apply {
                    addEventListener("mouseover", it.onMouseOver)
                    addEventListener("mouseout", it.onMouseOut)
                }

                // signalize to the drop target that dragging has started
                it.target.onDragStart()
            }
        }

        fun end() {
            currentDropTarget?.apply { onDrop(payload) }

            cleanup()
        }

        private fun cleanup() {
            contexts?.forEach {
                // remove mouse over event listeners
                it.target.parent?.dom?.apply {
                    removeEventListener("mouseover", it.onMouseOver)
                    removeEventListener("mouseout", it.onMouseOut)
                }

                // signalize to the drop target that dragging has ended
                it.target.onDragEnd()
            }

            contexts = null

            currentDropTarget = null
        }
    }

    private val dropTargets = mutableSetOf<DndDropTargetComponent<*>>()

    private var currentDrag: CurrentDrag<*>? = null

    internal fun registerDropTarget(target: DndDropTargetComponent<*>) {
        dropTargets.add(target)
    }

    internal fun removeDropTarget(target: DndDropTargetComponent<*>) {
        dropTargets.remove(target)
    }

    internal fun <PAYLOAD : Any> onDragStart(payload: PAYLOAD, type: KClass<PAYLOAD>) {

        @Suppress("UNCHECKED_CAST")
        val filtered = dropTargets
            .filter { it.acceptsPayloadClass(type) }
            .map { it as DndDropTargetComponent<PAYLOAD> }
            .filter { it.accepts(payload) }

        currentDrag = CurrentDrag(payload, filtered).apply { start() }
    }

    internal fun onDragEnd() {
        currentDrag?.apply { end() }
    }
}
