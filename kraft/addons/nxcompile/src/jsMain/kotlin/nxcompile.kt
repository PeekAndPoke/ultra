package io.peekandpoke.kraft.addons.nxcompile

/**
 * External binding for @nx-js/compiler-util, a sandboxed JavaScript code evaluation library.
 *
 * See: https://blog.risingstack.com/writing-a-javascript-framework-sandboxed-code-evaluation/
 */
@JsModule("@nx-js/compiler-util")
@JsNonModule
external object NxCompile {
    /**
     * Compiles the given [code] and returns a callable function
     */
    fun compileCode(code: String): dynamic
}
