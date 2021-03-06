package de.peekandpoke.ultra.kontainer

import java.time.Instant

data class KontainerDebugInfo(
    val services: List<ServiceDebugInfo>,
    val config: Map<String, String>
) {

    data class ServiceDebugInfo(
        val id: String,
        val type: ServiceProvider.Type,
        val instances: List<InstanceDebugInfo>
    )

    data class InstanceDebugInfo(
        val createdAt: Instant,
        val cls: String
    )
}
