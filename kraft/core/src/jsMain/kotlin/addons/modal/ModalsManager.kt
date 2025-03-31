package de.peekandpoke.kraft.addons.modal

import de.peekandpoke.kraft.addons.modal.ModalsManager.Handle
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.Unsubscribe

class ModalsManager : Stream<List<Handle>> {

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

    private val streamSource: StreamSource<List<Handle>> = StreamSource(emptyList())

    override fun invoke(): List<Handle> = streamSource()

    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    fun show(view: ModalRenderer): Handle {

        return Handle(id = ++handleCounter, view = view, dialogs = this).apply {
            streamSource.modify { plus(this@apply) }
        }
    }

    fun close(handle: Handle) {
        // call onClose handlers
        streamSource()
            .filter { it.id == handle.id }
            .forEach { it.onCloseHandlers.forEach { handler -> handler() } }

        // Remove from stack
        streamSource.modify {
            filterNot { it.id == handle.id }
        }
    }

    fun closeAll() {
        streamSource.modify { emptyList() }
    }
}

