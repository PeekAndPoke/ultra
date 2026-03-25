package io.peekandpoke.kraft.addons.sourcemappedstacktrace

import io.peekandpoke.kraft.utils.js

fun mapStackTraceCached(stack: String, done: (Array<String>) -> dynamic): dynamic {
    return mapStackTrace(
        stack,
        done,
        mapOf(
            "cacheGlobally" to true
        ).js
    )
}
