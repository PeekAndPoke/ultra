package io.peekandpoke.funktor.core.repair

import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorRepair() = module(Funktor_Repair)

internal val Funktor_Repair = module {

    // The facade
    dynamic(RepairMan::class)

    // Lifecycle
    singleton(RepairManOnAppStartingHook::class)

    // Cli
    dynamic(RepairRunCliCommand::class)
}
