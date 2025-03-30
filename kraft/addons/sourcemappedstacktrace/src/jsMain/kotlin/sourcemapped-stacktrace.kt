@file:JsModule("sourcemapped-stacktrace")
@file:JsNonModule

package de.peekandpoke.kraft.addons.sourcemappedstacktrace

external fun mapStackTrace(stack: String, done: (Array<String>) -> Unit, options: dynamic): dynamic
