package de.peekandpoke.funktor.core.repair

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorRepair() = module(Funktor_Repair)

internal val Funktor_Repair = module {

    // The facade
    dynamic(RepairMan::class)

    // Lifecycle
    singleton(RepairManOnAppStartingHook::class)

    // Cli
    dynamic(RepairRunCliCommand::class)
}
