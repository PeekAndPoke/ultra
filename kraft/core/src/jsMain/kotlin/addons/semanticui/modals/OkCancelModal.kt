package de.peekandpoke.kraft.addons.semanticui.modals

import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.debugId
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.ultra.semanticui.RenderFn
import de.peekandpoke.ultra.semanticui.SemanticTag
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.OkCancelModal(
    handle: ModalsManager.Handle,
    transition: FadingModal.Transition = FadingModal.Transition(),
    view: OkCancelModal.View,
    onResult: (OkCancelModal.Result) -> Unit,
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

class OkCancelModal(ctx: Ctx<Props>) : FadingModal<OkCancelModal.Props>(ctx) {

    companion object {
        operator fun invoke(block: Companion.() -> Unit) {
            this.block()
        }

        fun Tag.mini(
            handle: ModalsManager.Handle,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result) -> Unit,
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
            handle: ModalsManager.Handle,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result) -> Unit,
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
            handle: ModalsManager.Handle,
            header: RenderFn,
            content: RenderFn,
            okText: RenderFn = { +"Yes" },
            cancelText: RenderFn = { +"No" },
            onResult: (Result) -> Unit,
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

    data class Props(
        override val handle: ModalsManager.Handle,
        override val transition: Transition = Transition(),
        val view: View,
        val onResult: (Result) -> Unit,
    ) : FadingModal.Props()

    enum class Result {
        Ok,
        Cancel;

        val isOk: Boolean get() = this == Ok
        val isCancelled: Boolean get() = this == Cancel

        fun <R> handle(onOk: () -> R, onCancel: () -> R): R {
            return if (isOk) {
                onOk()
            } else {
                onCancel()
            }
        }

        fun <R> ifOk(onOk: () -> R): R? {
            return if (isOk) {
                onOk()
            } else {
                null
            }
        }

        fun <R> ifCancelled(onCancel: () -> R): R? {
            return if (isCancelled) {
                onCancel()
            } else {
                null
            }
        }
    }

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

    override fun FlowContent.renderContent() {
        ui.run(view.appearance).modal.transition.visible.active.front {
            debugId("ok-cancel-modal")

            view.header(this)
            view.content(this)
            noui.actions {
                ui.negative.button {
                    debugId("cancel-button")
                    onClick {
                        close()
                        props.onResult(Result.Cancel)
                    }
                    view.cancelText(this)
                }
                ui.positive.button {
                    debugId("ok-button")
                    onClick {
                        close {
                            props.onResult(Result.Ok)
                        }
                    }
                    view.okText(this)
                }
            }
        }
    }
}

