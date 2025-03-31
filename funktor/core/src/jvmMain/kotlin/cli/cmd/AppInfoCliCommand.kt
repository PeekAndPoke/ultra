package de.peekandpoke.funktor.core.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import de.peekandpoke.funktor.core.JsonPrinter
import de.peekandpoke.funktor.core.model.AppInfo

class AppInfoCliCommand(
    appInfo: Lazy<AppInfo>,
    jsonPrinter: Lazy<JsonPrinter>,
) : CliktCommand(name = "app:info") {

    private val appInfo by appInfo
    private val jsonPrinter by jsonPrinter

    override fun help(context: Context): String {
        return "Show App and System Info"
    }

    override fun run() {
        val obj = mapOf(
            "app" to appInfo,
            "java" to System.getProperties(),
            "environment" to System.getenv(),
        )

        println(
            jsonPrinter.prettyPrint(obj)
        )
    }
}
