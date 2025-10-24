package de.peekandpoke.kraft.semanticui

import de.peekandpoke.kraft.toasts.ToastRenderer
import de.peekandpoke.ultra.common.model.Message
import de.peekandpoke.ultra.html.key
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.SemanticFn
import de.peekandpoke.ultra.semanticui.SemanticIconFn
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.semantic
import de.peekandpoke.ultra.semanticui.semanticIcon
import de.peekandpoke.ultra.semanticui.ui

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
