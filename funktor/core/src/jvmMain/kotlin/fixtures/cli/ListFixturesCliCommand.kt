package de.peekandpoke.funktor.core.fixtures.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import de.peekandpoke.funktor.core.fixtures.FixtureInstaller

class ListFixturesCliCommand(
    installer: Lazy<FixtureInstaller>,
) : CliktCommand(name = "fixtures:list") {

    private val installer by installer

    override fun help(context: Context): String {
        return "Lists all fixtures"
    }

    override fun run(): Unit {
        println("Fixture Installer: ${installer::class.qualifiedName}")
        println()
        println("Available fixtures:")

        val prioritized = installer.getLoaders()

        prioritized.forEachIndexed { idx, loader ->
            println("${idx + 1}. ${loader::class.qualifiedName}")
        }
    }
}
