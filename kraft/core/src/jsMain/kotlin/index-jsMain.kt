package de.peekandpoke.kraft

import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.modal.ModalsStage
import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.popups.PopupsStage
import de.peekandpoke.kraft.addons.toasts.ToastsManager
import de.peekandpoke.kraft.addons.toasts.ToastsStage
import de.peekandpoke.ultra.common.datetime.kotlinx.initializeJsJodaTimezones
import kotlinx.html.FlowContent

@DslMarker
annotation class KraftDsl

class Kraft internal constructor() {
    companion object {
        /**
         * Initializes all external libraries and the returns [Kraft].
         */
        fun initialize(): Kraft {
            initializeJsJodaTimezones()

            return Kraft()
        }
    }

    val modals = ModalsManager()
    val popups = PopupsManager()
    val flash = ToastsManager()

    fun mount(tag: FlowContent, block: FlowContent.() -> Unit) {
        with(tag) {
            ModalsStage(modals)
            PopupsStage(popups)
            ToastsStage(flash)

            block()
        }
    }
}
