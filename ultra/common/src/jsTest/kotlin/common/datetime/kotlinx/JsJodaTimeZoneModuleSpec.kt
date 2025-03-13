package de.peekandpoke.ultra.common.datetime.kotlinx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

@Suppress("unused")
class JsJodaTimeZoneModuleSpec : StringSpec({

    "JsJodaTimeZoneModule needs to be loaded" {
        jsJodaTz.shouldNotBeNull()

        println(jsJodaTz)
    }

    "JsJodaTimeZoneModule initialize must work" {
        initializeJsJodaTimezones() shouldBe jsJodaTz
    }
})
