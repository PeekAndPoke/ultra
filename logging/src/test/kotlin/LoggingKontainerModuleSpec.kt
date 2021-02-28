package de.peekandpoke.ultra.logging

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class LoggingKontainerModuleSpec : StringSpec() {

    init {

        "Kontainer module must work" {

            val bluePrint = kontainer {
                ultraLogging()
            }

            val kontainer = bluePrint.useWith()

            kontainer.get(UltraLogManager::class).shouldBeInstanceOf<UltraLogManager>()
        }
    }
}
