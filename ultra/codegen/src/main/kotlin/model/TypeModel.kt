package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName

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
    /**
     * Positions where no type could be determined, so `unknown` would be emitted.
     *
     * Separate from [unresolved] because there is no [TypeId] to report — the failure is that a
     * container's element type, or a property's type, is not knowable at all. A non-empty list fails
     * validation: an `unknown` that nobody asked for is the Dart generator's silent `dynamic`, which is
     * the defect this module exists to remove. The deliberate way to get `unknown` is `claims.opaque`,
     * which is reported in the run summary.
     */
    val undetermined: List<Undetermined>,
) {
    /** A position whose type could not be determined at all. */
    data class Undetermined(
        /** How the position was reached, e.g. `InsightsApi.get -> Report.rows -> *`. */
        val path: List<String>,
        val reason: String,
    )

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

    private val byDeclKey: Map<String, TsTypeDecl> by lazy {
        decls.entries.associate { (id, decl) -> id.key to decl }
    }

    /**
     * The declaration for [cls], regardless of how it was instantiated.
     *
     * Declarations are keyed by the CLASS — `PageOf<Talk>` and `PageOf<Speaker>` share one — so
     * `decls[TypeId.of(typeOf<PageOf<Talk>>())]` never matches: that id's key carries the arguments.
     * Anything looking a declaration up from a `KType` must come through here.
     */
    fun declFor(cls: KClass<*>): TsTypeDecl? = byDeclKey[cls.qualifiedName ?: cls.jvmName]

    /** The declaration for [type]'s class, or `null` when its classifier is not a class. */
    fun declFor(type: KType): TsTypeDecl? = (type.classifier as? KClass<*>)?.let { declFor(it) }

    /** True when any of [qualifiedNames] is reachable in this model. */
    fun usesAny(vararg qualifiedNames: String): Boolean =
        qualifiedNames.any { name ->
            usedClaims.containsKey(name) || decls.keys.any { it.cls.qualifiedName == name }
        }
}
