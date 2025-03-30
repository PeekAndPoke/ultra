package de.peekandpoke.ktorfx.core.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.peekandpoke.ktorfx.core.App
import de.peekandpoke.ktorfx.core.AppArgs
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ultra.kontainer.Kontainer
import kotlin.system.exitProcess

class CliRunner(
    commands: Lazy<List<CliktCommand>>,
) {
    companion object {
        private const val DIVIDER =
            "-------------------------------------------------------------------------------------------------------"

        fun <C : AppConfig> create(
            app: App<C>,
            kontainer: (ctx: App<C>) -> Kontainer = { it.kontainers.system() },
        ) {
            try {
                val executor = kontainer(app).getOrNull(CliRunner::class)

                if (executor == null) {
                    println("[ERROR] The CliRunner is not available in the kontainer")
                    exitProcess(-1)
                }

                executor.run(app.args)

                exitProcess(0)

            } catch (e: Throwable) {

                println(DIVIDER)
                println("FAILED!")
                println(DIVIDER)
                e.printStackTrace()

                exitProcess(-1)
            }
        }
    }

    private class RootCmd(
        val title: String = "CLI interface",
        commands: List<CliktCommand>,
    ) : CliktCommand() {

        init {
            subcommands(
                commands.filter { it != this }
            )
        }

        override fun run() {
            println(title)
        }
    }

    private val commands by commands

    fun run(args: AppArgs) {

        val realArgs = when {
            args.args.none { it.isNotBlank() } -> listOf("--help")
            else -> args.args
        }

        val root = RootCmd(commands = commands.sortedBy { it.commandName })
        root.main(realArgs)

        println()
    }
}
