@file:Suppress("FunctionName", "ClassName", "unused")

package de.peekandpoke.kraft.jsbridges

import js.objects.Object

// TODO: move to kraft:addons:jwt

@JsModule("jwt-decode")
@JsNonModule
external object jwt_decode {
    fun default(jwt: String): Object?
}
