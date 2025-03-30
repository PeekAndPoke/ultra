package de.peekandpoke.kraft.addons.modal

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource

class ModalsManager {

    private var handleCounter = 0

    class Handle internal constructor(
        val id: Int,
        val view: ModalRenderer,
        internal val dialogs: ModalsManager,
    ) {
        internal val onCloseHandlers = mutableListOf<() -> Unit>()

        fun onClose(onClose: () -> Unit) = apply {
            onCloseHandlers.add(onClose)
        }

        fun close() = dialogs.close(this)
    }

    private val stack: MutableList<Handle> = mutableListOf()

    private val modalStream: StreamSource<List<Handle>> = StreamSource(emptyList())

    val current: Stream<List<Handle>> = modalStream.readonly

    fun show(view: ModalRenderer): Handle {

        return Handle(id = ++handleCounter, view = view, dialogs = this).apply {
            stack.add(this)
            notify()
        }
    }

    fun close(handle: Handle) {
        // call onClose handlers
        stack.filter { it.id == handle.id }
            .forEach { it.onCloseHandlers.forEach { handler -> handler() } }

        // Remove from stack
        stack.removeAll { it.id == handle.id }

        notify()
    }

    fun closeAll() {
        stack.clear()
        notify()
    }

    private fun notify() {
        modalStream(stack)
    }
}

