package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Variance

object GenericsUtil {
    operator fun <T> invoke(block: GenericsUtil.() -> T): T = block.invoke(this)

    /**
     * Map all type parameters of this KSType's declaration to their bound types,
     * keyed by the parameter name (e.g. "T", "R").
     *
     * - For star projections, we fall back to the first upper bound (if any).
     * - If the KSType is a typealias, we expand it before mapping.
     */
    fun KSType.typeArgumentMap(): Map<String, KSType?> {
        val expanded = this.expandTypeAliasIfAny()
        val decl = expanded.declaration as? KSClassDeclaration ?: return emptyMap()

        val params = decl.typeParameters
        val args = expanded.arguments

        // zip by index; ignore extras if something is malformed
        val pairs = params.zip(args)

        return buildMap {
            for ((param, arg) in pairs) {
                val name = param.name.asString()
                val bound: KSType? = when (arg.variance) {
                    Variance.STAR -> param.bounds.firstOrNull()?.resolve()
                    else -> arg.type?.resolve()
                }
                put(name, bound)
            }
        }
    }

    /**
     * Get the bound KSType for a given type-parameter NAME (e.g. "T").
     * Returns null if not present or if it's a star projection with no useful bound.
     */
    fun KSType.boundTypeOf(paramName: String): KSType? =
        typeArgumentMap()[paramName]

    /**
     * Get the bound KSType for a specific KSTypeParameter instance.
     * (Matches by parameter name within this type's declaration.)
     */
    fun KSType.boundTypeOf(param: KSTypeParameter): KSType? =
        boundTypeOf(param.name.asString())

    /**
     * Expand a KSType if it is a typealias; otherwise return itself.
     */
    fun KSType.expandTypeAliasIfAny(): KSType {
        var cur: KSType = this
        // Unwrap nested aliases, just in case
        while (cur.declaration is KSTypeAlias) {
            cur = (cur.declaration as KSTypeAlias).type.resolve()
        }
        return cur
    }
}
