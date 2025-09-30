package de.peekandpoke.kraft.common

import de.peekandpoke.kraft.common.ModalsManager.Handle
import de.peekandpoke.kraft.components.Automount
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.html.FlowContent

class ModalsManager : Stream<List<Handle>>, Automount {

    companion object {
        val key = TypedKey<ModalsManager>("modals")
    }

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

    private var handleCounter = 0

    private val streamSource: StreamSource<List<Handle>> = StreamSource(emptyList())

    override fun invoke(): List<Handle> = streamSource()

    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    override val priority = 1000

    override fun mount(flow: FlowContent) {
        with(flow) {
            ModalsStage(modals = this@ModalsManager)
        }
    }

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

