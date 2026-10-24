package io.peekandpoke.kraft.modals

import io.peekandpoke.kraft.KraftApp.AutoMountedUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.modals.ModalsManager.Handle
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.html.DIV
import kotlinx.html.FlowContent

/**
 * A manager for modal dialogs.
 *
 * Every modal answers exactly once: when it is gone, its [Handle.onResult] handlers get the decided answer, or `null`
 * when it was dismissed (backdrop, back navigation, [closeAll], or [Handle.close] without [Handle.resolve]).
 */
interface ModalsManager : Stream<List<Handle<*>>>, AutoMountedUi {
    companion object {
        val key = TypedKey<ModalsManager>("modals")

        val Component<*>.modals: ModalsManager get() = getAttributeRecursive(key)
    }

    /**
     * A handle to a modal dialog that answers with a [RESULT], or `null` when dismissed.
     *
     * Register handlers once, on the handle returned by [show] or in a component's `init`; never in the renderer,
     * which runs on every render of the modal stage.
     */
    class Handle<RESULT : Any> internal constructor(
        val id: Int,
        val view: ModalRenderer<RESULT>,
        internal val manager: ModalsManager,
    ) {
        private var decided = false
        private var result: RESULT? = null

        private val onResultHandlers = mutableListOf<(RESULT?) -> Unit>()
        private val onCloseHandlers = mutableListOf<() -> Unit>()

        /** The first call decides the answer, `null` decides a dismissal; later calls are ignored. Does not close. */
        fun resolve(result: RESULT?) {
            if (!decided) {
                decided = true
                this.result = result
            }
        }

        /** Called once when the modal is gone: the decided answer, or `null` if it was dismissed. */
        fun onResult(block: (RESULT?) -> Unit): Handle<RESULT> = apply {
            onResultHandlers.add(block)
        }

        /** Called once when the modal is gone, after the [onResult] handlers. */
        fun onClose(block: () -> Unit): Handle<RESULT> = apply {
            onCloseHandlers.add(block)
        }

        /** Removes the modal. The answer is what [resolve] decided, else `null`. */
        fun close() = manager.close(this)

        internal fun render(div: DIV) = view(div, this)

        /** Delivers the answer. Called by the manager only for a handle it has just removed from the stack. */
        internal fun deliver() {
            decided = true

            onResultHandlers.forEach { handler -> runHandler { handler(result) } }
            onCloseHandlers.forEach { handler -> runHandler { handler() } }
        }

        private fun runHandler(block: () -> Unit) {
            runCatching(block).onFailure { console.error("Modal handler failed", it) }
        }
    }

    /** Auto mount priority */
    override val autoMountPriority get() = 1000

    /** Mount the modals stage into the given flow */
    override fun autoMount(element: FlowContent)

    /** Show a modal dialog */
    fun <RESULT : Any> show(view: ModalRenderer<RESULT>): Handle<RESULT>

    /** Close a modal dialog represented by the given [Handle] */
    fun close(handle: Handle<*>)

    /** Close all modal dialogs, each answering `null` unless it was resolved */
    fun closeAll()
}

abstract class BaseModalsManager : ModalsManager {
    private var handleCounter = 0

    private val streamSource: StreamSource<List<Handle<*>>> = StreamSource(emptyList())

    /** The current state modals manager, containing all open modal dialogs */
    override fun invoke(): List<Handle<*>> = streamSource()

    /** Subscribe to changes of the current state modals manager */
    override fun subscribeToStream(sub: (List<Handle<*>>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    /** Auto mount priority */
    override val autoMountPriority = 1000

    /** Mount the modals stage into the given flow */
    override fun autoMount(element: FlowContent) {
        with(element) {
            ModalsStage(modals = this@BaseModalsManager)
        }
    }

    /** Show a modal dialog */
    override fun <RESULT : Any> show(view: ModalRenderer<RESULT>): Handle<RESULT> {
        return Handle(id = ++handleCounter, view = view, manager = this).apply {
            streamSource.modify { plus(this@apply) }
        }
    }

    /** Close a modal dialog represented by the given [Handle] */
    override fun close(handle: Handle<*>) {
        // Remove first, then deliver only what was removed: a second close() finds nothing and delivers nothing
        val removed = streamSource().filter { it.id == handle.id }

        if (removed.isNotEmpty()) {
            streamSource.modify { filterNot { it.id == handle.id } }
            removed.forEach { it.deliver() }
        }
    }

    /** Close all modal dialogs, each answering `null` unless it was resolved */
    override fun closeAll() {
        // Snapshot, oldest first: a modal opened by one of the handlers stays open
        val removed = streamSource()

        if (removed.isNotEmpty()) {
            streamSource.modify { filterNot { open -> removed.any { it.id == open.id } } }
            removed.forEach { it.deliver() }
        }
    }
}

/**
 * Default implementation of [ModalsManager].
 */
class DefaultModalsManager : BaseModalsManager()
