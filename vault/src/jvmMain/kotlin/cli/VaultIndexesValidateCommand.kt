package de.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class VaultIndexesValidateCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:indexes:validate") {

    private val database: Database by database

    override fun run() {
        runBlocking {
            database.validateIndexes()
        }
    }
}
