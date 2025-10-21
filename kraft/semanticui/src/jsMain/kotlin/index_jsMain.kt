package de.peekandpoke.kraft.semanticui

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.KraftDsl
import de.peekandpoke.kraft.common.ModalsManager
import de.peekandpoke.kraft.semanticui.popups.PopupsManager
import de.peekandpoke.kraft.semanticui.toasts.ToastsManager

@KraftDsl
fun KraftApp.Builder.semanticUI(
    toasts: ToastsManager.Builder.() -> Unit = {},
) = apply {
    toasts(toasts)
    popups()
    modals()
}

@KraftDsl
fun KraftApp.Builder.toasts(settings: ToastsManager.Builder.() -> Unit = {}) = apply {
    setAttribute(
        ToastsManager.key,
        ToastsManager(ToastsManager.Builder().apply(settings).build())
    )
}

@KraftDsl
fun KraftApp.Builder.popups() = apply {
    setAttribute(PopupsManager.key, PopupsManager())
}

@KraftDsl
fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, ModalsManager())
}
