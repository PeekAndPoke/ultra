package de.peekandpoke.ultra.foundation

import de.peekandpoke.ultra.foundation.spacetime.Kronos
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

@Suppress("unused")
fun KontainerBuilder.ultraFoundation() = module(Ultra_Foundation)

/**
 * Ultra Foundation kontainer module
 *
 * Provides the following services:
 *
 * - Kronos: a global date and time source
 */
val Ultra_Foundation = module {

    dynamic0(Kronos::class) { Kronos.systemUtc }
}
