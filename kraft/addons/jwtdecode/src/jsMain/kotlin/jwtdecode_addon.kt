package io.peekandpoke.kraft.addons.jwtdecode

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.utils.jsObjectToMap
import js.objects.Object
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded jwt-decode addon.
 *
 * Provides type-safe access to JWT token decoding.
 */
class JwtDecodeAddon internal constructor(
    private val jwtDecodeModule: dynamic,
) {
    /** Decodes the [jwt] and returns a JavaScript object. */
    fun decodeJwt(jwt: String): Object? {
        return jwtDecodeModule(jwt) as? Object
    }

    /** Decodes the [jwt] and returns a Kotlin [Map]. */
    fun decodeJwtAsMap(jwt: String): Map<String, Any?> {
        return jsObjectToMap(
            decodeJwt(jwt)
        )
    }
}

/** Key for the jwt-decode addon. */
val jwtDecodeAddonKey = AddonKey<JwtDecodeAddon>("jwtdecode")

/** Registers the jwt-decode addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.jwtDecode(lazy: Boolean = false): Addon<JwtDecodeAddon> = register(
    key = jwtDecodeAddonKey,
    name = "jwtdecode",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val jwtDecodeModule: dynamic = (js("import('jwt-decode')") as Promise<dynamic>).await()

    JwtDecodeAddon(
        jwtDecodeModule = jwtDecodeModule.default ?: jwtDecodeModule,
    )
}

/** Accessor for the jwt-decode addon on the registry. */
val AddonRegistry.jwtDecode: Addon<JwtDecodeAddon>
    get() = this[jwtDecodeAddonKey]
