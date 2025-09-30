package de.peekandpoke.kraft.semanticui.toasts

import de.peekandpoke.kraft.components.Automount
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.toasts.ToastsManager.Handle
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.model.Message
import de.peekandpoke.ultra.common.model.Messages
import de.peekandpoke.ultra.semanticui.SemanticFn
import de.peekandpoke.ultra.semanticui.SemanticIconFn
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.semantic
import de.peekandpoke.ultra.semanticui.semanticIcon
import de.peekandpoke.ultra.semanticui.ui
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.window
import kotlinx.html.FlowContent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias ToastRenderer = FlowContent.(Handle) -> Unit

class ToastsManager(
    val settings: Settings,
) : Stream<List<Handle>>, Automount {

    companion object {
        val key = TypedKey<ToastsManager>("toasts")

        val DefaultRenderer: ToastRenderer = { handle ->

            val styleFn: SemanticFn = when (handle.message.type) {
                Message.Type.info -> semantic { green }
                Message.Type.warning -> semantic { warning }
                Message.Type.error -> semantic { error }
            }

            val iconFn: SemanticIconFn = when (handle.message.type) {
                Message.Type.info -> semanticIcon { icon.check_circle }
                Message.Type.warning -> semanticIcon { icon.exclamation_circle }
                Message.Type.error -> semanticIcon { icon.exclamation_circle }
            }

            ui.floating.toastBox.transition.visible {
                ui.styleFn().toast.compact.visible {
                    key = handle.id.toString()
                    onClick { handle.close() }
                    icon.iconFn().render()
                    noui.content { +handle.message.text }
                }
            }
        }
    }

    data class Settings(
        val defaultDuration: Duration?,
        val defaultRenderer: ToastRenderer,
        val stageOptions: ToastsStage.Options,
    )

    class Builder internal constructor() {
        var defaultDuration: Duration? = 7.seconds
        var defaultRenderer: ToastRenderer = DefaultRenderer
        var stageOptions: ToastsStage.Options = ToastsStage.Options()

        internal fun build() = Settings(
            defaultDuration = defaultDuration,
            defaultRenderer = defaultRenderer,
            stageOptions = stageOptions,
        )
    }

    data class Handle(
        val id: Int,
        val message: Toast,
        val view: ToastRenderer,
        internal val flashMessages: ToastsManager,
    ) {
        fun close() = flashMessages.close(this)
    }

    private var handleCounter = 0

    private val source = StreamSource<List<Handle>>(emptyList())

    override fun invoke(): List<Handle> = source()

    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = source.subscribeToStream(sub)

    override val priority = 1000

    override fun mount(flow: FlowContent) {
        with(flow) {
            ToastsStage(toasts = this@ToastsManager)
        }
    }

    fun info(
        text: String,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        append(
            message = Toast.info(text = text, duration = duration),
            renderer = renderer,
        )
    }

    fun warning(
        text: String,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        append(
            message = Toast.warning(text = text, duration = duration),
            renderer = renderer,
        )
    }

    fun error(
        text: String,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        append(
            message = Toast.error(text = text, duration = duration),
            renderer = renderer,
        )
    }

    fun append(
        type: Message.Type,
        text: String,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) = append(
        message = Toast(type = type, text = text, duration = duration),
        renderer = renderer,
    )

    fun append(
        messages: Messages,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        messages.getAllMessages().forEach { append(it, duration, renderer) }
    }

    fun append(
        message: Message,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        append(
            message = Toast(type = message.type, text = message.text, duration = duration),
            renderer = renderer,
        )
    }

    fun append(
        message: Toast,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        val next = message.nextHandle(renderer)

        // trigger removal of this message after the timeout
        message.duration?.let { duration ->
            val ms = duration.inWholeMilliseconds.toInt()
            window.setTimeout({ close(next) }, ms)
        }

        source.modify {
            plus(next)
        }
    }

    fun close(handle: Handle) {
        source.modify {
            filterNot { it.id == handle.id }
        }
    }

    private fun Toast.nextHandle(renderer: ToastRenderer): Handle {
        val handleId = ++handleCounter

        console.log("new handle: $handleId")

        return Handle(
            id = handleId,
            message = this,
            view = renderer,
            flashMessages = this@ToastsManager,
        )
    }
}
