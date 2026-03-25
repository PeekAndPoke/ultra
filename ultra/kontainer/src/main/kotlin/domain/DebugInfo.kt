package io.peekandpoke.ultra.kontainer.domain

import io.peekandpoke.ultra.kontainer.InjectionType
import io.peekandpoke.ultra.kontainer.ServiceProvider
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Structured debug information about a [io.peekandpoke.ultra.kontainer.Kontainer] and its services.
 *
 * Obtained via [io.peekandpoke.ultra.kontainer.KontainerTools.getDebugInfo].
 */
data class DebugInfo(
    val services: List<ServiceDebugInfo>,
) {

    /** Debug information for a single registered service */
    data class ServiceDebugInfo(
        val cls: ClassInfo,
        val type: ServiceProvider.Type,
        val definition: ServiceDefinitionInfo,
        val instances: List<InstanceDebugInfo>,
    )

    /** Details about a service definition, including what it creates and its dependencies */
    data class ServiceDefinitionInfo(
        val creates: ClassInfo,
        val injectionType: InjectionType,
        val injects: List<ParamInfo>,
        val codeLocation: CodeLocation,
        val overwrites: ServiceDefinitionInfo?,
    ) {
        /** Source code location where the service was registered */
        data class CodeLocation(
            val location: String,
        )
    }

    /** Debug information about a created service instance */
    data class InstanceDebugInfo(
        val createdAt: Instant,
        val cls: ClassInfo,
    )

    /** Represents a class by its fully qualified name */
    data class ClassInfo(
        val fqn: String,
    ) {
        companion object {
            fun <T : Any> of(cls: KClass<T>): ClassInfo = ClassInfo(
                fqn = cls.java.name
            )

            fun of(type: KType): ClassInfo? = when (val cls = type.classifier) {
                is KClass<*> -> of(cls)
                else -> null
            }
        }
    }

    /** Information about a parameter injected into a service */
    data class ParamInfo(
        val name: String,
        val provisionType: ProvisionType,
        val classes: List<ClassInfo>,
    ) {
        /** How a parameter is provided: directly or lazily */
        enum class ProvisionType {
            Direct,
            Lazy
        }
    }
}
