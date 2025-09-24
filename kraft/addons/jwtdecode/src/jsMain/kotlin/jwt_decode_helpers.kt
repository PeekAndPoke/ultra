package de.peekandpoke.kraft.addons

import de.peekandpoke.kraft.utils.jsObjectToMap
import js.objects.Object

/**
 * Decodes the [jwt] and returns a JavaScript object
 */
fun decodeJwt(jwt: String): Object? {
    return jwt_decode.default(jwt)
}

/**
 * Decodes the [jwt] and returns a kotlin [Map]
 */
fun decodeJwtAsMap(jwt: String): Map<String, Any?> {
    return jsObjectToMap(
        decodeJwt(jwt)
    )
}
