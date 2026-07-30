package io.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

/**
 * CLI command `vault:indexes:validate` — reports missing and excess indexes per repository, via
 * [Database.validateIndexes].
 *
 * Read-only: does not change the database. Always exits 0 and prints nothing itself; check the
 * driver's log output for the actual report.
 */
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
