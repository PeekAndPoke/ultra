package de.peekandpoke.ktorfx.rest

import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.rest.codec.RestCodec
import de.peekandpoke.ktorfx.rest.codec.SlumberRestCodec
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

fun KontainerBuilder.ktorFxRest(
    builder: KtorFXRestBuilder.() -> Unit = {},
) = module(KtorFX_Rest, builder)

inline val KontainerAware.restCodec: RestCodec get() = kontainer.get()
inline val ApplicationCall.restCodec: RestCodec get() = kontainer.restCodec
inline val RoutingContext.restCodec: RestCodec get() = call.restCodec

val KtorFX_Rest = module { builder: KtorFXRestBuilder.() -> Unit ->

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

    KtorFXRestBuilder(this).apply(builder)
}

class KtorFXRestBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun jwt(config: JwtConfig) {
        with(kontainer) {
            singleton(JwtGenerator::class) { JwtGenerator(config) }
        }
    }
}
