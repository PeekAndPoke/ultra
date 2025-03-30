package de.peekandpoke.ktorfx.core

import de.peekandpoke.ktorfx.core.cli.ktorFxCli
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.fixtures.ktorFxFixtures
import de.peekandpoke.ktorfx.core.lifecycle.AppLifeCycleHooks
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ktorfx.core.model.CacheBuster
import de.peekandpoke.ktorfx.core.repair.ktorFxRepair
import de.peekandpoke.ktorfx.core.session.NullCurrentSession
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.logging.ultraLogging
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.security.ultraSecurity
import de.peekandpoke.ultra.security.user.User
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.vault.ultraVault
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun KontainerBuilder.ktorFxCore(config: AppConfig, info: AppInfo) = module(KtorFX_Core, config, info)

inline val KontainerAware.kronos: Kronos get() = kontainer.get()
inline val ApplicationCall.kronos: Kronos get() = kontainer.kronos
inline val RoutingContext.kronos: Kronos get() = call.kronos

inline val KontainerAware.appInfo: AppInfo get() = kontainer.get(AppInfo::class)
inline val ApplicationCall.appInfo: AppInfo get() = kontainer.appInfo
inline val RoutingContext.appInfo: AppInfo get() = call.appInfo

inline val KontainerAware.appConfig: AppConfig get() = kontainer.get(AppConfig::class)
inline val ApplicationCall.appConfig: AppConfig get() = kontainer.appConfig
inline val RoutingContext.appConfig: AppConfig get() = call.appConfig
inline fun <reified T : AppConfig> RoutingContext.appConfig(): T = call.appConfig as T

inline val KontainerAware.cacheBuster: CacheBuster get() = kontainer.get(CacheBuster::class)
inline val ApplicationCall.cacheBuster: CacheBuster get() = kontainer.cacheBuster
inline val RoutingContext.cacheBuster: CacheBuster get() = call.cacheBuster

inline val KontainerAware.jsonPrinter: JsonPrinter get() = kontainer.get()
inline val ApplicationCall.jsonPrinter: JsonPrinter get() = kontainer.jsonPrinter
inline val RoutingContext.jsonPrinter: JsonPrinter get() = call.jsonPrinter

inline val KontainerAware.passwordHasher: PasswordHasher get() = kontainer.get(PasswordHasher::class)
inline val ApplicationCall.passwordHasher: PasswordHasher get() = kontainer.passwordHasher
inline val RoutingContext.passwordHasher: PasswordHasher get() = call.passwordHasher

inline val KontainerAware.jwtGenerator: JwtGenerator get() = kontainer.get()
inline val ApplicationCall.jwtGenerator: JwtGenerator get() = kontainer.jwtGenerator
inline val RoutingContext.jwtGenerator: JwtGenerator get() = call.jwtGenerator

inline val KontainerAware.user: User get() = userProvider.invoke()
inline val ApplicationCall.user: User get() = kontainer.user
inline val RoutingContext.user: User get() = call.user

inline val KontainerAware.userProvider: UserProvider get() = kontainer.get(UserProvider::class)
inline val ApplicationCall.userProvider: UserProvider get() = kontainer.userProvider
inline val RoutingContext.userProvider: UserProvider get() = call.userProvider

/**
 * Common kontainer module
 */
val KtorFX_Core = module { config: AppConfig, info: AppInfo ->

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
    ultraSecurity(config.ktorFx.security)
    ultraVault()

    // KtorFX sub-modules
    ktorFxCli()
    ktorFxRepair()
    ktorFxFixtures(config)
}


