package de.peekandpoke.kraft.semanticui

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.KraftDsl
import de.peekandpoke.kraft.modals.modals
import de.peekandpoke.kraft.popups.popups
import de.peekandpoke.kraft.semanticui.popups.SemanticUiPopupComponent
import de.peekandpoke.kraft.toasts.ToastsManager
import de.peekandpoke.kraft.toasts.ToastsStage
import de.peekandpoke.kraft.toasts.toasts

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
