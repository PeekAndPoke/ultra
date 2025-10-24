package de.peekandpoke.kraft.modals

import de.peekandpoke.kraft.components.AutoMountedUi
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import de.peekandpoke.kraft.modals.ModalsManager.Handle
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.html.FlowContent

/**
 * A manager for modal dialogs.
 */
interface ModalsManager : Stream<List<Handle>>, AutoMountedUi {
    companion object {
        val key = TypedKey<ModalsManager>("modals")

        val Component<*>.modals: ModalsManager get() = getAttributeRecursive(key)
    }

    /**
     * A handle to a modal dialog.
     */
    class Handle internal constructor(
        val id: Int,
        val view: ModalRenderer,
        internal val manager: ModalsManager,
    ) {
        internal val onCloseHandlers = mutableListOf<() -> Unit>()

        /** Registers a callback which is called when the modal dialog is closed. */
        fun onClose(onClose: () -> Unit) = apply {
            onCloseHandlers.add(onClose)
        }

        /** Closes the modal dialog. */
        fun close() = manager.close(this)
    }

    /** Auto mount priority */
    override val priority get() = 1000

    /** Mount the modals stage into the given flow */
    override fun mount(flow: FlowContent)

    /** Show a modal dialog */
    fun show(view: ModalRenderer): Handle

    /** Close a modal dialog represented by the given [Handle] */
    fun close(handle: Handle)

    /** Close all modal dialogs */
    fun closeAll()
}

abstract class BaseModalsManager : ModalsManager {
    private var handleCounter = 0

    private val streamSource: StreamSource<List<Handle>> = StreamSource(emptyList())

    /** The current state modals manager, containing all open modal dialogs */
    override fun invoke(): List<Handle> = streamSource()

    /** Subscribe to changes of the current state modals manager */
    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    /** Auto mount priority */
    override val priority = 1000

    /** Mount the modals stage into the given flow */
    override fun mount(flow: FlowContent) {
        with(flow) {
            ModalsStage(modals = this@BaseModalsManager)
        }
    }

    /** Show a modal dialog */
    override fun show(view: ModalRenderer): Handle {
        return Handle(id = ++handleCounter, view = view, manager = this).apply {
            streamSource.modify { plus(this@apply) }
        }
    }

    /** Close a modal dialog represented by the given [Handle] */
    override fun close(handle: Handle) {
        // call onClose handlers
        streamSource()
            .filter { it.id == handle.id }
            .forEach { it.onCloseHandlers.forEach { handler -> handler() } }

        // Remove from stack
        streamSource.modify {
            filterNot { it.id == handle.id }
        }
    }

    /** Close all modal dialogs */
    override fun closeAll() {
        streamSource.modify { emptyList() }
    }
}

/**
 * Default implementation of [ModalsManager].
 */
class DefaultModalsManager : BaseModalsManager()
