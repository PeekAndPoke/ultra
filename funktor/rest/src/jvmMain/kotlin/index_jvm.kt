package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.rest.codec.RestCodec
import de.peekandpoke.funktor.rest.codec.SlumberRestCodec
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.cache.FastCache
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer.Companion.withSlumberCache
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.funktorRest(
    config: AppConfig,
    builder: FunktorRestBuilder.() -> Unit = {},
) = module(Funktor_Rest, config, builder)

inline val KontainerAware.restCodec: RestCodec get() = kontainer.get()
inline val ApplicationCall.restCodec: RestCodec get() = kontainer.restCodec
inline val RoutingContext.restCodec: RestCodec get() = call.restCodec


val Funktor_Rest = module { config: AppConfig, builder: FunktorRestBuilder.() -> Unit ->

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
