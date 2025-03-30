package de.peekandpoke.ktorfx.core.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ktorfx.core.JsonPrinter
import de.peekandpoke.ktorfx.core.config.AppConfig

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
