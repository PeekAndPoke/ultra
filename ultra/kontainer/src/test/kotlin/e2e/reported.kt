package io.peekandpoke.ultra.kontainer.e2e

import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.ServiceType
import kotlin.reflect.KClass

/** What a provider reports about its service, as one comparable value. */
data class Reported(val type: ServiceType, val kind: ServiceKind, val overridden: Boolean = false) {

    companion object {
        val StaticSingleton = Reported(ServiceType.Singleton, ServiceKind.Static)
        val SemiDynamicSingleton = Reported(ServiceType.Singleton, ServiceKind.SemiDynamic)
        val DynamicSingleton = Reported(ServiceType.Singleton, ServiceKind.Dynamic)
        val OverriddenDynamicSingleton = Reported(ServiceType.Singleton, ServiceKind.Dynamic, overridden = true)
        val StaticPrototype = Reported(ServiceType.Prototype, ServiceKind.Static)
        val SemiDynamicPrototype = Reported(ServiceType.Prototype, ServiceKind.SemiDynamic)
        val DynamicPrototype = Reported(ServiceType.Prototype, ServiceKind.Dynamic)

        /** Parses `Type.Kind`, e.g. `Prototype.SemiDynamic`. */
        fun parse(text: String): Reported {
            val (type, kind) = text.split(".")
            return Reported(ServiceType.valueOf(type), ServiceKind.valueOf(kind))
        }
    }

    override fun toString(): String = "$type.$kind" + if (overridden) " (overridden)" else ""
}

/** What this provider reports. */
val ServiceProvider.reported: Reported get() = Reported(type = type, kind = kind, overridden = overridden)

/** What the kontainer reports for [cls]. */
fun <T : Any> Kontainer.reportedOf(cls: KClass<T>): Reported = getProvider(cls).reported
