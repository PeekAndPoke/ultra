package de.peekandpoke.kraft.jsbridges

import de.peekandpoke.kraft.utils.jsObjectToMap
import js.objects.Object

/**
 * Decodes the [jwt] and returns a javascript object
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
