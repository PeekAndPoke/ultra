package io.peekandpoke.funktor.core.fixtures.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.peekandpoke.funktor.core.fixtures.FixtureInstaller
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.vault.Database
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
                clear()
            }
        }

        // Reset the kronos
        kontainer.use(Kronos::class) {
            (this as? Kronos.Mutable)?.set(Kronos.systemUtc)
        }

        // We instantiate another kontainer, e.g. to get a fresh instance of Kronos
        kontainer.clone().let {

            it.use(FixtureInstaller::class) {
                val result = clearAndInstall()

                run {
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
