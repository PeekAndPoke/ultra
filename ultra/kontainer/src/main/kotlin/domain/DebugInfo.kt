package de.peekandpoke.ultra.kontainer.domain

import de.peekandpoke.ultra.kontainer.InjectionType
import de.peekandpoke.ultra.kontainer.ServiceProvider
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

data class DebugInfo(
    val services: List<ServiceDebugInfo>,
) {

    data class ServiceDebugInfo(
        val cls: ClassInfo,
        val type: ServiceProvider.Type,
        val definition: ServiceDefinitionInfo,
        val instances: List<InstanceDebugInfo>,
    )

    data class ServiceDefinitionInfo(
        val creates: ClassInfo,
        val injectionType: InjectionType,
        val injects: List<ParamInfo>,
        val codeLocation: CodeLocation,
        val overwrites: ServiceDefinitionInfo?,
    ) {
        data class CodeLocation(
            val location: String,
        )
    }

    data class InstanceDebugInfo(
        val createdAt: Instant,
        val cls: ClassInfo,
    )

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

    data class ParamInfo(
        val name: String,
        val provisionType: ProvisionType,
        val classes: List<ClassInfo>,
    ) {
        enum class ProvisionType {
            Direct,
            Lazy
        }
    }
}
