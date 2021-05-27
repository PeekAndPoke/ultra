package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

internal object KontainerCoroutineScope : CoroutineScope {
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Unconfined)

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext
}
