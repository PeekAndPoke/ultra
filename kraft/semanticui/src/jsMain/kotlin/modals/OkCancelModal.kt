package io.peekandpoke.kraft.semanticui.modals

import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.ultra.html.RenderFn
import io.peekandpoke.ultra.html.debugId
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.SemanticTag
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import org.w3c.dom.HTMLElement

/**
 * Factory function that renders an [OkCancelModal] dialog.
 *
 * @param handle The modal handle from [ModalsManager] used to control the modal lifecycle.
 * @param transition Fade-in/out transition timing.
 * @param view Defines the modal's appearance, header, content, and button labels.
 * @param onResult Called once after the modal is gone: [OkCancelModal.Result.Ok], [OkCancelModal.Result.Cancel],
 *   or `null` when it was dismissed (backdrop, back navigation, `closeAll()`). It is wired when the modal first
 *   renders; for an answer even when the modal closes before that, register `onResult` on the handle `show` returns.
 */
@Suppress("FunctionName")
fun Tag.OkCancelModal(
    handle: ModalsManager.Handle<OkCancelModal.Result>,
    transition: FadingModal.Transition = FadingModal.Transition(),
    view: OkCancelModal.View,
    onResult: (OkCancelModal.Result?) -> Unit = {},
) = comp(
    OkCancelModal.Props(
        handle = handle,
        transition = transition,
        view = view,
        onResult = onResult
    )
) {
    OkCancelModal(it)
}

/** A confirmation modal with Ok and Cancel buttons, built on [FadingModal]. */
class OkCancelModal(ctx: Ctx<Props>) : FadingModal<OkCancelModal.Props, OkCancelModal.Result>(ctx) {

    companion object {
        operator fun invoke(block: Companion.() -> Unit) {
            this.block()
        }

        fun Tag.mini(
            handle: ModalsManager.Handle<Result>,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result?) -> Unit = {},
        ) =
            OkCancelModal(
                handle = handle,
                onResult = onResult,
                view = View(
                    appearance = { mini },
                    header = header,
                    content = content,
                    okText = okText,
                    cancelText = cancelText,
                )
            )

        fun Tag.tiny(
            handle: ModalsManager.Handle<Result>,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result?) -> Unit = {},
        ) =
            OkCancelModal(
                handle = handle,
                onResult = onResult,
                view = View(
                    appearance = { tiny },
                    header = header,
                    content = content,
                    okText = okText,
                    cancelText = cancelText,
                )
            )

        fun Tag.small(
            handle: ModalsManager.Handle<Result>,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result?) -> Unit = {},
        ) =
            OkCancelModal(
                handle = handle,
                onResult = onResult,
                view = View(
                    appearance = { small },
                    header = header,
                    content = content,
                    okText = okText,
                    cancelText = cancelText,
                )
            )
    }

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** Props for [OkCancelModal]. */
    data class Props(
        override val handle: ModalsManager.Handle<Result>,
        override val transition: Transition = Transition(),
        val view: View,
        val onResult: (Result?) -> Unit,
    ) : FadingModal.Props<Result>()

    /** The result of the Ok/Cancel modal interaction. */
    enum class Result {
        Ok,
        Cancel;

        val isOk: Boolean get() = this == Ok
        val isCancelled: Boolean get() = this == Cancel

        /** Dispatches to [onOk] or [onCancel] based on the result. */
        fun <R> handle(onOk: () -> R, onCancel: () -> R): R {
            return if (isOk) {
                onOk()
            } else {
                onCancel()
            }
        }

        /** Executes [onOk] only if the result is [Ok], returning null otherwise. */
        fun <R> ifOk(onOk: () -> R): R? {
            return if (isOk) {
                onOk()
            } else {
                null
            }
        }

        /** Executes [onCancel] only if the result is [Cancel], returning null otherwise. */
        fun <R> ifCancelled(onCancel: () -> R): R? {
            return if (isCancelled) {
                onCancel()
            } else {
                null
            }
        }
    }

    /** Defines the visual structure of the Ok/Cancel modal: appearance, header, content, and button labels. */
    data class View(
        val appearance: SemanticTag.() -> SemanticTag,
        val header: RenderFn,
        val content: RenderFn,
        val okText: RenderFn,
        val cancelText: RenderFn,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    val view get() = props.view

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        // Registered once, reading props at delivery time
        props.handle.onResult { props.onResult(it) }

        lifecycle {
            onMount {
                // Cancel has the focus: Enter must not confirm a destructive action by accident
                dom?.querySelector(".actions button.negative")?.unsafeCast<HTMLElement>()?.focus()
            }
        }
    }

    override fun FlowContent.renderContent() {
        ui.run(view.appearance).modal.transition.visible.active.front {
            debugId("ok-cancel-modal")

            view.header(this)
            view.content(this)
            noui.actions {
                ui.negative.button Button {
                    type = ButtonType.button
                    debugId("cancel-button")
                    onClick { close(Result.Cancel) }
                    view.cancelText(this)
                }
                ui.positive.button Button {
                    type = ButtonType.button
                    debugId("ok-button")
                    onClick { close(Result.Ok) }
                    view.okText(this)
                }
            }
        }
    }
}
