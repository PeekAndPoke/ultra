package de.peekandpoke.ultra.streams.ops

/** Creates a debounced function using browser setTimeout */
fun debouncedFunc(delayMs: Int = 200, block: () -> Unit): () -> Unit {
    val timer = DebouncingTimer(delayMs)

    return {
        timer.invoke(block)
    }
}

/** Creates a debounced function with a shorter delay for the first call */
fun debouncedFuncExceptFirst(delayMs: Int = 200, block: () -> Unit): () -> Unit {
    val timer = DebouncingTimer(delayMs = delayMs, delayFirstMs = 3)

    return {
        timer.invoke(block)
    }
}
