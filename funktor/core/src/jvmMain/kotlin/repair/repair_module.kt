package de.peekandpoke.ktorfx.core.repair

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.ktorFxRepair() = module(KtorFxRepairModule)

internal val KtorFxRepairModule = module {

    // The facade
    dynamic(RepairMan::class)

    // Lifecycle
    singleton(RepairManOnAppStartingHook::class)

    // Cli
    dynamic(RepairRunCliCommand::class)
}
