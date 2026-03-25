package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging

class DatabaseSpec : StringSpec() {

    private val k
        get() = kontainer {
            singleton(Kronos::class) { Kronos.systemUtc }

            ultraLogging()
            ultraVault(VaultConfig())
        }.create()

    init {
        "Simple creation of a Database" {
            k.get(Database::class).shouldBeInstanceOf<Database>()
        }
    }
}
