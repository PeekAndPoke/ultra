package de.peekandpoke.kraft.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

//private val dispatcher = Dispatchers.Unconfined
private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun <T : Any?> launch(block: suspend () -> T): Job {
    return scope.launch {
        block()
    }
}

fun <T : Any?> async(block: suspend () -> T): Deferred<T> {
    return scope.async {
        block()
    }
}
