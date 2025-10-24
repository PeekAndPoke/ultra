package de.peekandpoke.funktor.core.fixtures.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import de.peekandpoke.funktor.core.fixtures.FixtureInstaller
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class InstallFixturesCliCommand(
    private val kontainer: Kontainer,
    private val log: Log,
) : CliktCommand(name = "fixtures:install") {

    override fun help(context: Context): String {
        return "Installs the fixtures"
    }

    override fun run(): Unit = runBlocking {

        // We first clear all repositories
        kontainer.clone().let {

            it.use(Database::class) {
                log.info("Ensuring database repos")
                ensureRepositories()
            }

            it.use(FixtureInstaller::class) {
                log.info("Clearing database repos")
                runBlocking {
                    clear()
                }
            }
        }

        // Reset the kronos
        kontainer.use(Kronos::class) {
            (this as? Kronos.Mutable)?.set(Kronos.systemUtc)
        }

        // We instantiate another kontainer, e.g. to get a fresh instance of Kronos
        kontainer.clone().let {

            it.use(FixtureInstaller::class) {
                val result = runBlocking {
                    clearAndInstall()
                }

                runBlocking {
                    delay(100)

                    log.info("Done installing fixtures!")

                    result.loaders.forEach { loader ->
                        log.info("[${if (loader.isOk) "OK" else "FAILED"}] ${loader.loader::class.qualifiedName}")
                    }

                    if (!result.successful) {
                        log.error("ERROR: installing fixtures failed !")
                    }

                    log.info("Installing fixtures to ${result.duration}")
                }
            }
        }
    }
}
