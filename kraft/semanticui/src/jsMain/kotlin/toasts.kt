package io.peekandpoke.kraft.semanticui

import io.peekandpoke.kraft.toasts.ToastRenderer
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.model.Message
import io.peekandpoke.ultra.semanticui.SemanticFn
import io.peekandpoke.ultra.semanticui.SemanticIconFn
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.semantic
import io.peekandpoke.ultra.semanticui.semanticIcon
import io.peekandpoke.ultra.semanticui.ui

val SemanticUiToastsRenderer: ToastRenderer = { handle ->

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
