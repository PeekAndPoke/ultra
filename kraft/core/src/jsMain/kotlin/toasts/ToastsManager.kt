package io.peekandpoke.kraft.toasts

import io.peekandpoke.kraft.components.AutoMountedUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.toasts.ToastsManager.Handle
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.model.Message
import io.peekandpoke.ultra.model.Messages
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.window
import kotlinx.css.Border
import kotlinx.css.BorderStyle
import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.Padding
import kotlinx.css.backgroundColor
import kotlinx.css.border
import kotlinx.css.display
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.span
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Renders a toast notification, receiving the [Handle] to allow closing. */
typealias ToastRenderer = FlowContent.(Handle) -> Unit

/**
 * Manages toast notifications: displaying, auto-dismissing, and closing them.
 */
class ToastsManager(
    val settings: Settings,
) : Stream<List<Handle>>, AutoMountedUi {

    companion object {
        val key = TypedKey<ToastsManager>("toasts")

        /** Retrieves the [ToastsManager] from the component tree. */
        val Component<*>.toasts: ToastsManager get() = getAttributeRecursive(key)

        /** A basic fallback toast renderer using colored divs. */
        val DefaultToastsRenderer: ToastRenderer = { handle ->

            div {
                css {
                    padding = Padding(8.px)
                    display = Display.inlineBlock
                    border = Border(1.px, BorderStyle.solid, Color.grey)

                    backgroundColor = when (handle.message.type) {
                        Message.Type.info -> Color.green
                        Message.Type.warning -> Color.yellow
                        Message.Type.error -> Color.red
                    }
                }

                key = handle.id.toString()
                onClick { handle.close() }

                span { +handle.message.text }
            }
        }
    }

    /** Configuration for the toasts manager. */
    data class Settings(
        val defaultDuration: Duration?,
        val defaultRenderer: ToastRenderer,
        val stageOptions: ToastsStage.Options,
    )

    /** Builder for configuring [ToastsManager] settings. */
    class Builder internal constructor() {
        var defaultDuration: Duration? = 7.seconds
        var defaultRenderer: ToastRenderer = DefaultToastsRenderer
        var stageOptions: ToastsStage.Options = ToastsStage.Options()

        internal fun build() = ToastsManager(
            settings = Settings(
                defaultDuration = defaultDuration,
                defaultRenderer = defaultRenderer,
                stageOptions = stageOptions,
            )
        )
    }

    /** A handle to an active toast notification. */
    data class Handle(
        val id: Int,
        val message: Toast,
        val view: ToastRenderer,
        internal val flashMessages: ToastsManager,
    ) {
        /** Closes this toast. */
        fun close() = flashMessages.close(this)
    }

    private var handleCounter = 0

    private val source = StreamSource<List<Handle>>(emptyList())

    override fun invoke(): List<Handle> = source()

    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = source.subscribeToStream(sub)

    override val autoMountPriority = 1000

    override fun autoMount(element: FlowContent) {
        with(element) {
            ToastsStage(toasts = this@ToastsManager)
        }
    }

    /** Shows an info toast with the given [text]. */
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

    /** Shows a warning toast with the given [text]. */
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

    /** Shows an error toast with the given [text]. */
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

    /** Appends a toast with the specified [type] and [text]. */
    fun append(
        type: Message.Type,
        text: String,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) = append(
        message = Toast(type = type, text = text, duration = duration),
        renderer = renderer,
    )

    /** Appends toasts for all messages in the given [messages] container. */
    fun append(
        messages: Messages,
        duration: Duration? = settings.defaultDuration,
        renderer: ToastRenderer = settings.defaultRenderer,
    ) {
        messages.getAllMessages().forEach { append(it, duration, renderer) }
    }

    /** Appends a toast from a [Message] model object. */
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

    /** Appends a [Toast] and schedules auto-dismissal if a duration is set. */
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

    /** Removes the toast identified by the given [handle]. */
    fun close(handle: Handle) {
        source.modify {
            filterNot { it.id == handle.id }
        }
    }

    private fun Toast.nextHandle(renderer: ToastRenderer): Handle {
        val handleId = ++handleCounter

//        console.log("new handle: $handleId")

        return Handle(
            id = handleId,
            message = this,
            view = renderer,
            flashMessages = this@ToastsManager,
        )
    }
}
