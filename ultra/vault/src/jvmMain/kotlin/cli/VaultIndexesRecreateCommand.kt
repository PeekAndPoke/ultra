package io.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

/**
 * CLI command `vault:indexes:re-create` — drops every non-primary index and re-creates it from the
 * current definitions, via [Database.recreateIndexes].
 *
 * Destructive: on a live database, queries lose index support for the window between drop and
 * re-create. Always exits 0 and prints nothing itself; check the driver's log output for failures.
 */
class VaultIndexesRecreateCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:indexes:re-create") {

    private val database: Database by database

    override fun run() {
        runBlocking {
            database.recreateIndexes()
        }
    }
}
