package io.peekandpoke.ultra.codegen.model

/**
 * The frozen result of walking the type graph.
 *
 * Produced by [TypeWalker], consumed by validation and then by the emitters. Every reference inside
 * is symbolic, so the model is complete and order-independent by construction.
 */
data class TypeModel(
    /** Declarations in discovery order, keyed by id. */
    val decls: Map<TypeId, TsTypeDecl>,
    /** Claims that were actually reached, keyed by qualified name. */
    val usedClaims: Map<String, TsTypeClaim>,
    /** Types the walker could not classify — a non-empty list fails validation. */
    val unresolved: List<Unresolved>,
    /** Types reachable as a Kotlin `Long`, which loses precision above 2^53 once JSON-parsed. */
    val longValued: List<Reached>,
) {
    /** A type the walker reached but could not turn into a declaration. */
    data class Unresolved(
        val id: TypeId,
        /** How the type was reached, e.g. `FunktorConfApi.getTalks -> Talk.startsAt`. */
        val path: List<String>,
        val reason: String,
    )

    /** A type reached at a given path, used for advisory reporting. */
    data class Reached(
        val id: TypeId,
        val path: List<String>,
    )

    /** The declaration for [id], or `null`. */
    operator fun get(id: TypeId): TsTypeDecl? = decls[id]

    /** True when any of [qualifiedNames] is reachable in this model. */
    fun usesAny(vararg qualifiedNames: String): Boolean =
        qualifiedNames.any { name ->
            usedClaims.containsKey(name) || decls.keys.any { it.cls.qualifiedName == name }
        }
}
