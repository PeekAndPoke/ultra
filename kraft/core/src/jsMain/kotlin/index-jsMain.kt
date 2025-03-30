package de.peekandpoke.kraft

import de.peekandpoke.ultra.common.datetime.kotlinx.initializeJsJodaTimezones

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
}
