@file:Suppress("FunctionName", "ClassName", "unused")

package io.peekandpoke.kraft.addons

import js.objects.Object

@JsModule("jwt-decode")
@JsNonModule
/** External binding for the jwt-decode library. */
external object jwt_decode {
    /** Decodes the given [jwt] token and returns its payload as a JS object. */
    fun default(jwt: String): Object?
}
