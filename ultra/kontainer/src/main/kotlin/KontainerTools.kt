package io.peekandpoke.ultra.kontainer

import io.peekandpoke.ultra.common.maxLineLength
import io.peekandpoke.ultra.kontainer.domain.DebugInfo

/**
 * Debugging and introspection tools for a [Kontainer].
 *
 * Accessible via [Kontainer.tools].
 */
class KontainerTools internal constructor(
    /** The kontainer being inspected */
    val kontainer: Kontainer,
) {
    /** The underlying service provider factory */
    val factory: ServiceProviderFactory get() = kontainer.getServiceProviderFactory()

    /** The blueprint that created the kontainer */
    val blueprint: KontainerBlueprint get() = kontainer.blueprint

    /**
     * Collects structured debug information about all services in the kontainer.
     */
    fun getDebugInfo(): DebugInfo {

        fun ServiceDefinition.toInfo(): DebugInfo.ServiceDefinitionInfo {
            return DebugInfo.ServiceDefinitionInfo(
                creates = DebugInfo.ClassInfo.of(producer.creates),
                injectionType = injectionType,
                injects = producer.params.map { parameter ->
                    DebugInfo.ParamInfo(
                        name = parameter.name,
                        provisionType = when (parameter.getProvisionType()) {
                            ParameterProvider.ProvisionType.Direct -> DebugInfo.ParamInfo.ProvisionType.Direct
                            ParameterProvider.ProvisionType.Lazy -> DebugInfo.ParamInfo.ProvisionType.Lazy
                        },
                        classes = parameter.getInjectedServiceTypes(blueprint).map {
                            DebugInfo.ClassInfo.of(it)
                        },
                    )
                },
                codeLocation = DebugInfo.ServiceDefinitionInfo.CodeLocation(
                    location = codeLocation.stack.firstOrNull()?.toString() ?: "n/a",
                ),
                overwrites = overwrites?.toInfo(),
            )
        }

        val services = factory.getAllProviders().map { (cls, provider) ->
            DebugInfo.ServiceDebugInfo(
                cls = DebugInfo.ClassInfo.of(cls),
                type = provider.type,
                instances = provider.instances.map { instance ->
                    DebugInfo.InstanceDebugInfo(
                        createdAt = instance.createdAt,
                        cls = DebugInfo.ClassInfo.of(instance.instance::class),
                    )
                },
                definition = provider.definition.toInfo(),
            )
        }

        return DebugInfo(services = services)
    }

    /**
     * Returns a human-readable table of all services, their types, and current instances.
     */
    fun dumpKontainer(): String {

        val rows = mutableListOf(
            Triple("Service ID", "Type", "Instances")
        )

        rows.addAll(
            factory.getAllProviders().map { (k, v) ->
                Triple(
                    k.getName(),
                    v.type.toString(),
                    v.instances.joinToString("\n") { "${it.createdAt}: ${it.instance::class.getName()}" }
                )
            }
        )

        val lens = Triple(
            rows.maxOfOrNull { it.first.maxLineLength() } ?: 0,
            rows.maxOfOrNull { it.second.maxLineLength() } ?: 0,
            rows.maxOfOrNull { it.third.maxLineLength() } ?: 0
        )

        val services = rows.map {
            "${it.first.padEnd(lens.first)} | ${it.second.padEnd(lens.second)} | ${it.third.padEnd(lens.third)}"
        }

        return "Kontainer dump:" +
                "\n\n" +
                services.joinToString("\n")
    }
}
