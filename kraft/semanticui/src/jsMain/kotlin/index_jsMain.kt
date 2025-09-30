package de.peekandpoke.kraft.semanticui

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.common.ModalsManager
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import de.peekandpoke.kraft.semanticui.popups.PopupsManager
import de.peekandpoke.kraft.semanticui.toasts.ToastsManager

fun KraftApp.Builder.semanticUI(
    toasts: ToastsManager.Builder.() -> Unit = {},
) = apply {
    toasts(toasts)
    popups()
    modals()
}

fun KraftApp.Builder.toasts(settings: ToastsManager.Builder.() -> Unit = {}) = apply {
    setAttribute(
        ToastsManager.key,
        ToastsManager(ToastsManager.Builder().apply(settings).build())
    )
}

val Component<*>.toasts: ToastsManager get() = getAttributeRecursive(ToastsManager.key)

fun KraftApp.Builder.popups() = apply {
    setAttribute(PopupsManager.key, PopupsManager())
}

val Component<*>.popups: PopupsManager get() = getAttributeRecursive(PopupsManager.key)

fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, ModalsManager())
}

val Component<*>.modals: ModalsManager get() = getAttributeRecursive(ModalsManager.key)

