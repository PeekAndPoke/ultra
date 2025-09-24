@file:Suppress("FunctionName", "ClassName", "unused")

package de.peekandpoke.kraft.addons

import js.objects.Object

@JsModule("jwt-decode")
@JsNonModule
external object jwt_decode {
    fun default(jwt: String): Object?
}
