package de.peekandpoke.funktor.core.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.funktor.core.JsonPrinter
import de.peekandpoke.funktor.core.config.AppConfig

class AppConfigCliCommand(
    config: Lazy<AppConfig>,
    jsonPrinter: Lazy<JsonPrinter>,
) : CliktCommand(name = "app:config") {

    private val config by config
    private val jsonPrinter by jsonPrinter

    override fun run() {
        println(
            jsonPrinter.prettyPrint(config)
        )
    }
}
