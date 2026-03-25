package io.peekandpoke.funktor.core.broker

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.broker.vault.IncomingJavaTimeConverter
import io.peekandpoke.funktor.core.broker.vault.IncomingMpDateTimeConverter
import io.peekandpoke.funktor.core.broker.vault.IncomingPrimitiveConverter
import io.peekandpoke.funktor.core.broker.vault.IncomingVaultConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingJavaTimeConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingMpDateTimeConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingPrimitiveConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingVaultConverter
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

/** Helper for importing [Funktor_Broker] into a [KontainerBuilder] */
fun KontainerBuilder.funktorBroker() = module(Funktor_Broker)

/** Get the [IncomingConverter] from the kontainer */
inline val KontainerAware.incomingConverter: IncomingConverter get() = kontainer.get()

/** Get the [IncomingConverter] from the kontainer */
inline val ApplicationCall.incomingConverter: IncomingConverter get() = kontainer.incomingConverter

/** Get the [IncomingConverter] from the kontainer */
inline val RoutingContext.incomingConverter: IncomingConverter get() = call.incomingConverter

/** Get the [TypedRouteRenderer] from the kontainer */
inline val KontainerAware.typedRouteRenderer: TypedRouteRenderer get() = kontainer.get()

/** Get the [TypedRouteRenderer] from the kontainer */
inline val ApplicationCall.typedRouteRenderer: TypedRouteRenderer get() = kontainer.typedRouteRenderer

/** Get the [TypedRouteRenderer] from the kontainer */
inline val RoutingContext.typedRouteRenderer: TypedRouteRenderer get() = call.typedRouteRenderer

/**
 * Broker kontainer module for type safe routing.
 */
val Funktor_Broker
    get() = module {

        singleton(IncomingConverterLookup::class)
        dynamic(IncomingConverter::class)
        dynamic(IncomingVaultConverter::class)
        singleton(IncomingMpDateTimeConverter::class)
        singleton(IncomingPrimitiveConverter::class)
        singleton(IncomingJavaTimeConverter::class)

        dynamic(TypedRouteRenderer::class)
        singleton(OutgoingConverter::class)
        singleton(OutgoingVaultConverter::class)
        singleton(OutgoingPrimitiveConverter::class)
        singleton(OutgoingMpDateTimeConverter::class)
        singleton(OutgoingJavaTimeConverter::class)
    }
