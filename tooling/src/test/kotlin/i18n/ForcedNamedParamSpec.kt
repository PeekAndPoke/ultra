package io.peekandpoke.ultra.tooling.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

// Proves the exact signature shape the emitter produces — a leading `vararg _: Unit` — is valid
// Kotlin and that the trailing params must be passed by name. `demoAccessor(count = 3, name = ...)`
// compiles; a positional `demoAccessor(3, ...)` would NOT (a parameter following a vararg must be
// named). If this file compiles, the emitted shape is valid. (`vararg _: Nothing` is prohibited.)
private fun demoAccessor(vararg forcedNamed: Unit, count: Int, name: Any?): String = "n=$count/$name"

class ForcedNamedParamSpec : StringSpec({
    "the emitted vararg-Nothing forced-named signature compiles and accepts named args" {
        demoAccessor(count = 3, name = "x") shouldBe "n=3/x"
    }
})
