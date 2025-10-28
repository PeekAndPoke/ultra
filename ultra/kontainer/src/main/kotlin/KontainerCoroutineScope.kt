package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

internal object KontainerCoroutineScope : CoroutineScope {
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + scopeJob)

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext
}
