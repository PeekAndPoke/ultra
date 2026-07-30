package io.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

/**
 * CLI command `vault:indexes:ensure` — brings indexes in line with their definitions, via
 * [Database.ensureIndexes].
 *
 * Not purely additive: missing indexes are created, but an index whose definition changed is
 * dropped and re-created rather than left in place. Always exits 0 and prints nothing itself;
 * check the driver's log output for failures.
 */
class VaultIndexesEnsureCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:indexes:ensure") {

    private val database: Database by database

    override fun run() {
        runBlocking {
            database.ensureIndexes()
        }
    }
}
