package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

internal object KontainerCoroutineScope {
    val job = Job()
    val scope = CoroutineScope(job + Dispatchers.Unconfined)
}
