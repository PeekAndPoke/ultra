package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.kontainer.kontainer

class LoggingKontainerModuleSpec : StringSpec() {

    init {

        "Kontainer module must work" {

            val bluePrint = kontainer {
                ultraLogging()
            }

            val kontainer = bluePrint.create()

            kontainer.get(UltraLogManager::class).shouldBeInstanceOf<UltraLogManager>()
        }
    }
}
