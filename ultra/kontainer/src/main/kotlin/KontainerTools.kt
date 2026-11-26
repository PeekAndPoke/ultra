package io.peekandpoke.ultra.kontainer

import io.peekandpoke.ultra.common.maxLineLength
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import kotlin.reflect.KClass

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
                type = type,
                kind = kind,
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
                kind = provider.kind,
                overridden = provider.overridden,
                semiDynamicBecauseOf = blueprint.semiDynamicBecauseOf(cls).map { DebugInfo.ClassInfo.of(it) },
                created = provider.created.let {
                    DebugInfo.CreatedDebugInfo(count = it.count, firstAt = it.firstAt, lastAt = it.lastAt)
                },
                definition = provider.definition.toInfo(),
            )
        }

        return DebugInfo(services = services)
    }

    /**
     * The kind as the dump shows it, with the override flag or what made the service semi dynamic.
     */
    private fun kindCell(cls: KClass<*>, provider: ServiceProvider): String = when {
        provider.overridden -> "${provider.kind} (overridden)"

        provider.kind == ServiceKind.SemiDynamic ->
            "${provider.kind} (" +
                    blueprint.semiDynamicBecauseOf(cls).joinToString(", ") { it.simpleName ?: it.getName() } + ")"

        else -> provider.kind.toString()
    }

    /**
     * The creations as the dump shows them: the count with the first and the last time; empty when none.
     */
    private fun createdCell(created: ServiceProvider.Created): String = when (created.count) {
        0L -> ""
        else -> "${created.count}, first ${created.firstAt}, last ${created.lastAt}"
    }

    /**
     * Returns a human-readable table of all services: their type, kind and what each provider created.
     */
    fun dumpKontainer(): String {

        val rows = mutableListOf(
            listOf("Service ID", "Type", "Kind", "Created")
        )

        rows.addAll(
            factory.getAllProviders().map { (k, v) ->
                listOf(
                    k.getName(),
                    v.type.toString(),
                    kindCell(k, v),
                    createdCell(v.created),
                )
            }
        )

        val lens = rows.first().indices.map { col -> rows.maxOf { it[col].maxLineLength() } }

        val services = rows.map { row ->
            row.mapIndexed { col, cell -> cell.padEnd(lens[col]) }.joinToString(" | ")
        }

        return "Kontainer dump:" +
                "\n\n" +
                services.joinToString("\n")
    }
}
