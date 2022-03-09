package de.peekandpoke.ultra.kontainer

import java.time.Instant

data class KontainerDebugInfo(
    val services: List<ServiceDebugInfo>,
    val config: Map<String, String>
) {

    data class ServiceDebugInfo(
        val cls: String,
        val type: ServiceProvider.Type,
        val definition: ServiceDefinitionInfo,
        val instances: List<InstanceDebugInfo>,
    )

    data class ServiceDefinitionInfo(
        val createsCls: String,
        val injectionType: InjectionType,
        val codeLocation: CodeLocation,
        val overwrites: ServiceDefinitionInfo?,
    ) {
        data class CodeLocation(
            val stackTrace: String,
        )
    }

    data class InstanceDebugInfo(
        val createdAt: Instant,
        val cls: String
    )
}
