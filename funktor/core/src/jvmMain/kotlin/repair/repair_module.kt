package io.peekandpoke.funktor.core.repair

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

inline val KontainerAware.repairMan: RepairMan get() = kontainer.get()
inline val ApplicationCall.repairMan: RepairMan get() = kontainer.repairMan
inline val RoutingContext.repairMan: RepairMan get() = call.repairMan

internal fun KontainerBuilder.funktorRepair() = module(Funktor_Repair)

internal val Funktor_Repair = module {

    // The facade
    dynamic(RepairMan::class)

    // Lifecycle
    singleton(RepairManOnAppStartingHook::class)

    // Cli
    dynamic(RepairRunCliCommand::class)
}
