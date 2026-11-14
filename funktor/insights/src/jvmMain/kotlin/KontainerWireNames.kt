package io.peekandpoke.funktor.insights

import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType

/**
 * A provider as the insights wire names it: `Singleton`, `SemiDynamic`, `Dynamic`, `Prototype` or `DynamicOverride`.
 *
 * Records on disk and both GUIs read these names, so they stay what they were before Kontainer split its injection
 * type into [ServiceType] and [ServiceKind]. Every prototype is `Prototype`, whatever its kind.
 */
internal fun wireProviderType(type: ServiceType, kind: ServiceKind, overridden: Boolean): String = when {
    overridden -> "DynamicOverride"

    type == ServiceType.Prototype -> "Prototype"

    else -> when (kind) {
        ServiceKind.Static -> "Singleton"
        ServiceKind.SemiDynamic -> "SemiDynamic"
        ServiceKind.Dynamic -> "Dynamic"
    }
}

/**
 * A definition's declared type as the insights wire names it: `Singleton`, `Dynamic` or `Prototype`.
 */
internal fun wireInjectionType(type: ServiceType, kind: ServiceKind): String = when (type) {
    ServiceType.Prototype -> "Prototype"

    ServiceType.Singleton -> when (kind) {
        ServiceKind.Dynamic -> "Dynamic"
        ServiceKind.Static, ServiceKind.SemiDynamic -> "Singleton"
    }
}
