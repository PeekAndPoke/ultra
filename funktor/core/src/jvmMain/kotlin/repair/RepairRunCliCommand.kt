package de.peekandpoke.ktorfx.core.repair

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class RepairRunCliCommand(
    repairMan: Lazy<RepairMan>,
) : CliktCommand(name = "repair:run") {

    private val repairMan by repairMan

    override fun help(context: Context): String {
        return "Runs all repairs"
    }

    override fun run() {
        repairMan.run()
    }
}
