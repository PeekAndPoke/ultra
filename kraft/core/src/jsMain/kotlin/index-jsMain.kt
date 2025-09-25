package de.peekandpoke.kraft

import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.popups.PopupsStage
import de.peekandpoke.kraft.addons.toasts.ToastsManager
import de.peekandpoke.kraft.addons.toasts.ToastsStage
import de.peekandpoke.kraft.common.ModalsManager
import de.peekandpoke.kraft.common.ModalsStage
import de.peekandpoke.ultra.common.datetime.kotlinx.initializeJsJodaTimezones
import kotlinx.html.FlowContent

@DslMarker
annotation class KraftDsl

fun kraftApp(block: KraftApp.Builder.() -> Unit = {}) = KraftApp.Builder().apply(block).build()

class KraftApp internal constructor(
    settings: Settings,
) {
    class Settings(
        val toasts: ToastsManager.Settings,
    )

    class Builder internal constructor() {
        private val toasts = ToastsManager.Builder()

        fun toasts(block: ToastsManager.Builder.() -> Unit) = toasts.apply(block)

        internal fun build() = KraftApp(
            Settings(
                toasts = toasts.build()
            )
        )
    }

    init {
        initializeJsJodaTimezones()
    }

    val modals = ModalsManager()
    val popups = PopupsManager()
    val toasts = ToastsManager(settings.toasts)

    fun mount(tag: FlowContent, block: FlowContent.() -> Unit) {
        with(tag) {
            ModalsStage(modals)
            PopupsStage(popups)
            ToastsStage(toasts)

            block()
        }
    }
}
