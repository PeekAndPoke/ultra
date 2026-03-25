package io.peekandpoke.ultra.vault.cli

import com.github.ajalt.clikt.core.CliktCommand
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class VaultRepositoriesEnsureCommand(
    database: Lazy<Database>,
) : CliktCommand(name = "vault:repositories:ensure") {

    private val database: Database by database

    override fun run() = runBlocking {
        database.ensureRepositories()
    }
}
