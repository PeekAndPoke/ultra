package io.peekandpoke.funktor.core.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.peekandpoke.funktor.core.JsonPrinter
import io.peekandpoke.funktor.core.config.AppConfig

class AppConfigCliCommand(
    config: Lazy<AppConfig>,
    jsonPrinter: Lazy<JsonPrinter>,
) : CliktCommand(name = "app:config") {

    private val config by config
    private val jsonPrinter by jsonPrinter

    override fun help(context: Context): String {
        return "Show app config"
    }

    override fun run() {
        println(
            jsonPrinter.prettyPrint(config)
        )
    }
}
