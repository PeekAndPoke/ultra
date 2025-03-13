package de.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class VaultIndexesRecreateCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:indexes:re-create") {

    private val database: Database by database

    override fun run() = runBlocking {
        database.recreateIndexes()
    }
}
