package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.rest.codec.RestCodec
import de.peekandpoke.funktor.rest.codec.SlumberRestCodec
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.funktorRest(
    builder: FunktorRestBuilder.() -> Unit = {},
) = module(Funktor_Rest, builder)

inline val KontainerAware.restCodec: RestCodec get() = kontainer.get()
inline val ApplicationCall.restCodec: RestCodec get() = kontainer.restCodec
inline val RoutingContext.restCodec: RestCodec get() = call.restCodec

val Funktor_Rest = module { builder: FunktorRestBuilder.() -> Unit ->

    val codecConfig = SlumberConfig.default.prependModules(VaultSlumberModule)

    dynamic(RestCodec::class) { database: Database?, entityCache: EntityCache? ->
        SlumberRestCodec(
            config = codecConfig,
            attributes = TypedAttributes.of {
                if (database != null) {
                    add(VaultSlumberModule.DatabaseKey, database)
                }

                if (entityCache != null) {
                    add(VaultSlumberModule.EntityCacheKey, entityCache)
                }
            },
        )
    }

    FunktorRestBuilder(this).apply(builder)
}

class FunktorRestBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun jwt(config: JwtConfig?) {
        if (config == null) return

        with(kontainer) {
            singleton(JwtGenerator::class) { JwtGenerator(config) }
        }
    }
}
