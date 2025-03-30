package de.peekandpoke.kraft.addons.nxcompile

// see https://blog.risingstack.com/writing-a-javascript-framework-sandboxed-code-evaluation/

@JsModule("@nx-js/compiler-util")
@JsNonModule
external object NxCompile {
    /**
     * Compiles the given [code] and returns a callable function
     */
    fun compileCode(code: String): dynamic
}

