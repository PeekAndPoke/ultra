package io.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

/**
 * CLI command `vault:repositories:ensure` — creates any missing repository collections and ensures
 * their indexes, via [Database.ensureRepositories].
 *
 * Index handling is not purely additive: an index whose definition changed is dropped and
 * re-created, not just supplemented (see [Database.ensureIndexes]). Always exits 0 and prints
 * nothing itself; check the driver's log output for failures.
 */
class VaultRepositoriesEnsureCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:repositories:ensure") {

    private val database: Database by database

    override fun run() = runBlocking {
        database.ensureRepositories()
    }
}
