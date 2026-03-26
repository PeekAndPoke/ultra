@file:JsModule("sourcemapped-stacktrace")
@file:JsNonModule

package io.peekandpoke.kraft.addons.sourcemappedstacktrace

/** Maps a minified [stack] trace back to original source locations, calling [done] with the result. */
external fun mapStackTrace(stack: String, done: (Array<String>) -> Unit, options: dynamic): dynamic
