package io.peekandpoke.kraft.semanticui

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.modals.modals
import io.peekandpoke.kraft.popups.popups
import io.peekandpoke.kraft.semanticui.popups.SemanticUiPopupComponent
import io.peekandpoke.kraft.toasts.ToastsManager
import io.peekandpoke.kraft.toasts.ToastsStage
import io.peekandpoke.kraft.toasts.toasts

@KraftDsl
fun KraftApp.Builder.semanticUI(
    toasts: ToastsManager.Builder.() -> Unit = {},
) = apply {
    modals()

    // Configure popups to work with Semantic UI
    popups {
        popupFactory { target, positioning, handle, content ->
            SemanticUiPopupComponent(target, positioning, handle, content)
        }
    }

    // Configure toasts to work with Semantic UI
    toasts {
        stageOptions = ToastsStage.Options(
            cssClasses = listOf("ui", "toast-container", "top", "right")
        )
        defaultRenderer = SemanticUiToastsRenderer
        this.toasts()
    }
}
