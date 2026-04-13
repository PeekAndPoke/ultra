package io.peekandpoke.funktor.core

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.peekandpoke.funktor.core.cli.funktorCli
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.fixtures.funktorFixtures
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.CacheBuster
import io.peekandpoke.funktor.core.repair.funktorRepair
import io.peekandpoke.funktor.core.session.NullCurrentSession
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.ultraSecurity
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.vault.ultraVault

/** Registers the Funktor core module (config, lifecycle, security, vault, fixtures) in the kontainer. */
fun KontainerBuilder.funktorCore(
    config: AppConfig,
    info: AppInfo,
) = module(Funktor_Core, config, info)

/** Gets [Kronos] from the kontainer. */
inline val KontainerAware.kronos: Kronos get() = kontainer.get()

/** Gets [Kronos] from the kontainer. */
inline val ApplicationCall.kronos: Kronos get() = kontainer.kronos

/** Gets [Kronos] from the kontainer. */
inline val RoutingContext.kronos: Kronos get() = call.kronos

/** Gets [AppInfo] from the kontainer. */
inline val KontainerAware.appInfo: AppInfo get() = kontainer.get(AppInfo::class)

/** Gets [AppInfo] from the kontainer. */
inline val ApplicationCall.appInfo: AppInfo get() = kontainer.appInfo

/** Gets [AppInfo] from the kontainer. */
inline val RoutingContext.appInfo: AppInfo get() = call.appInfo

/** Gets [AppConfig] from the kontainer. */
inline val KontainerAware.appConfig: AppConfig get() = kontainer.get(AppConfig::class)

/** Gets [AppConfig] from the kontainer. */
inline val ApplicationCall.appConfig: AppConfig get() = kontainer.appConfig

/** Gets [AppConfig] from the kontainer. */
inline val RoutingContext.appConfig: AppConfig get() = call.appConfig

/** Gets [AppConfig] cast to [T] from the kontainer. */
inline fun <reified T : AppConfig> RoutingContext.appConfig(): T = call.appConfig as T

/** Gets [CacheBuster] from the kontainer. */
inline val KontainerAware.cacheBuster: CacheBuster get() = kontainer.get(CacheBuster::class)

/** Gets [CacheBuster] from the kontainer. */
inline val ApplicationCall.cacheBuster: CacheBuster get() = kontainer.cacheBuster

/** Gets [CacheBuster] from the kontainer. */
inline val RoutingContext.cacheBuster: CacheBuster get() = call.cacheBuster

/** Gets [JsonPrinter] from the kontainer. */
inline val KontainerAware.jsonPrinter: JsonPrinter get() = kontainer.get()

/** Gets [JsonPrinter] from the kontainer. */
inline val ApplicationCall.jsonPrinter: JsonPrinter get() = kontainer.jsonPrinter

/** Gets [JsonPrinter] from the kontainer. */
inline val RoutingContext.jsonPrinter: JsonPrinter get() = call.jsonPrinter

/** Gets [PasswordHasher] from the kontainer. */
inline val KontainerAware.passwordHasher: PasswordHasher get() = kontainer.get(PasswordHasher::class)

/** Gets [PasswordHasher] from the kontainer. */
inline val ApplicationCall.passwordHasher: PasswordHasher get() = kontainer.passwordHasher

/** Gets [PasswordHasher] from the kontainer. */
inline val RoutingContext.passwordHasher: PasswordHasher get() = call.passwordHasher

/** Gets [JwtGenerator] from the kontainer. */
inline val KontainerAware.jwtGenerator: JwtGenerator get() = kontainer.get()

/** Gets [JwtGenerator] from the kontainer. */
inline val ApplicationCall.jwtGenerator: JwtGenerator get() = kontainer.jwtGenerator

/** Gets [JwtGenerator] from the kontainer. */
inline val RoutingContext.jwtGenerator: JwtGenerator get() = call.jwtGenerator

/** Gets the current [User] from the kontainer's [UserProvider]. */
inline val KontainerAware.user: User get() = userProvider.invoke()

/** Gets the current [User] from the kontainer's [UserProvider]. */
inline val ApplicationCall.user: User get() = kontainer.user

/** Gets the current [User] from the kontainer's [UserProvider]. */
inline val RoutingContext.user: User get() = call.user

/** Gets the [UserProvider] from the kontainer. */
inline val KontainerAware.userProvider: UserProvider get() = kontainer.get(UserProvider::class)

/** Gets the [UserProvider] from the kontainer. */
inline val ApplicationCall.userProvider: UserProvider get() = kontainer.userProvider

/** Gets the [UserProvider] from the kontainer. */
inline val RoutingContext.userProvider: UserProvider get() = call.userProvider

/**
 * Common kontainer module
 */
val Funktor_Core = module { config: AppConfig, info: AppInfo ->

    // App config
    instance(config)
    // AppInfo
    instance(info)
    // Serialization / Json / etc...
    instance(JsonPrinter)

    // Kronos
    dynamic(Kronos::class) { Kronos.systemUtc }
    // A default cache buster.
    dynamic(CacheBuster::class) { appInfo: AppInfo -> CacheBuster.of(appInfo) }
    // session
    dynamic(CurrentSession::class) { NullCurrentSession }

    ////  LifeCycle  ////////////////////////////////////////////////////////////////////////
    singleton(AppLifeCycleHooks::class)

    ////  Modules  //////////////////////////////////////////////////////////////////////////
    ultraLogging()
    ultraSecurity(config.funktor.security)
    ultraVault(config.funktor.vault)

    // Funktor sub-modules
    funktorCli()
    funktorRepair()
    funktorFixtures(config)
}
