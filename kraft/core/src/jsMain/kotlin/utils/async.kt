package io.peekandpoke.kraft.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

//private val dispatcher = Dispatchers.Unconfined
private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

/** Launches a coroutine on the main dispatcher and returns its [Job]. */
fun <T : Any?> launch(block: suspend () -> T): Job {
    return scope.launch {
        block()
    }
}

/** Launches a coroutine on the main dispatcher and returns a [Deferred] result. */
fun <T : Any?> async(block: suspend () -> T): Deferred<T> {
    return scope.async {
        block()
    }
}
