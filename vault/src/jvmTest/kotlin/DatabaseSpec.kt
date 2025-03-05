package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.logging.ultraLogging
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class DatabaseSpec : StringSpec() {

    private val k
        get() = kontainer {
            singleton(Kronos::class) { Kronos.systemUtc }

            ultraLogging()
            ultraVault()
        }.create()

    init {
        "Simple creation of a Database" {
            k.get(Database::class).shouldBeInstanceOf<Database>()
        }
    }
}
