package io.peekandpoke.funktor.rest

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.rest.codec.RestCodec
import io.peekandpoke.funktor.rest.codec.SlumberRestCodec
import io.peekandpoke.ultra.cache.FastCache
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.jwt.JwtConfig
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer.Companion.withSlumberCache
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.slumber.VaultSlumberModule

/** Registers the Funktor REST module (codec, JWT) in the kontainer. */
fun KontainerBuilder.funktorRest(
    config: AppConfig,
    builder: FunktorRestBuilder.() -> Unit = {},
) = module(Funktor_Rest, config, builder)

/** Gets the [RestCodec] from the kontainer. */
inline val KontainerAware.restCodec: RestCodec get() = kontainer.get()

/** Gets the [RestCodec] from the kontainer. */
inline val ApplicationCall.restCodec: RestCodec get() = kontainer.restCodec

/** Gets the [RestCodec] from the kontainer. */
inline val RoutingContext.restCodec: RestCodec get() = call.restCodec

val Funktor_Rest = module { config: AppConfig, builder: FunktorRestBuilder.() -> Unit ->

    singleton(ValidateRoutesOnAppStarting::class)

    val codecConfig = SlumberConfig.default.prependModules(VaultSlumberModule)

    val cacheMemory = Runtime.getRuntime().maxMemory() / 10

    val rawCache = FastCache.Builder<Any?, Any?>()
        .maxMemoryUsage(cacheMemory) // 128 MB
        .build()

    val slumberCache = DataClassSlumberer.SlumberCache(
        wrapped = rawCache,
        excludedClasses = setOf(ApiResponse::class, ApiResponse.Insights::class)
    )

    dynamic(RestCodec::class) { database: Database?, entityCache: EntityCache? ->
        SlumberRestCodec(
            config = codecConfig.plusAttributes(
                TypedAttributes.of {
                    if (database != null) {
                        add(VaultSlumberModule.DatabaseKey, database)
                    }

                    if (entityCache != null) {
                        add(VaultSlumberModule.EntityCacheKey, entityCache)
                    }
                },
            ).withSlumberCache(slumberCache)
        )
    }

    FunktorRestBuilder(kontainer = this, config = config).apply(builder)
}

/** DSL builder for configuring the REST module (JWT, etc.). */
class FunktorRestBuilder internal constructor(
    private val kontainer: KontainerBuilder,
    val config: AppConfig,
) {

    fun jwt() {
        jwt(config)
    }

    fun jwt(config: AppConfig) {
        jwt(config.funktor.auth.jwt)
    }

    fun jwt(config: JwtConfig?) {
        config ?: error("JwtConfig must not be null")

        with(kontainer) {
            singleton(JwtGenerator::class) { JwtGenerator(config) }
        }
    }
}
