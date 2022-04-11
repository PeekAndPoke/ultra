package de.peekandpoke.ultra.common.datetime.kotlinx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull

@Suppress("unused")
class JsJodaTimeZoneModuleSpec : StringSpec({

    "JsJodaTimeZoneModule needs to be loaded" {
        jsJodaTz.shouldNotBeNull()
    }
})
