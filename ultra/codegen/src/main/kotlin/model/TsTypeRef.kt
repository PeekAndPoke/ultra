package io.peekandpoke.ultra.codegen.model

/**
 * A reference to a type from a property, parameter or return position.
 *
 * References are symbolic: [Named] carries a [TypeId], never a resolved declaration. That is what
 * makes the model order-independent and lets circular types fall out for free — the old Dart gen
 * resolved references to nodes eagerly and silently degraded to `dynamic` when the target had not
 * been registered yet.
 */
sealed interface TsTypeRef {

    /**
     * A reference to a declared or claimed type, with its type arguments.
     *
     * [args] is empty for a non-generic type. A generic type is declared ONCE and referenced with
     * arguments — `PageOf<Talk>` in type position, `PageOf(Talk)` in schema position — rather than
     * monomorphized into a separate `PageOfTalk` declaration per instantiation.
     */
    data class Named(val id: TypeId, val args: List<TsTypeRef> = emptyList()) : TsTypeRef

    /**
     * A reference to a type parameter of the declaration this reference sits in.
     *
     * Only ever appears inside a generic declaration's own body: `items: T[]` in `PageOf<T>`.
     */
    data class TypeParam(val name: String) : TsTypeRef

    /**
     * A JSON array.
     *
     * Kotlin `List`, `Set` and arrays all slumber to a JSON array, so they share one reference shape.
     */
    data class ArrayOf(val item: TsTypeRef) : TsTypeRef

    /**
     * A JSON object used as a map.
     *
     * Keys are always strings on the wire regardless of the Kotlin key type, so only the value type
     * is carried.
     */
    data class RecordOf(val value: TsTypeRef) : TsTypeRef

    /** A nullable type, emitted as `T | null`. */
    data class Nullable(val inner: TsTypeRef) : TsTypeRef

    /** `string` */
    data object TsString : TsTypeRef

    /** `number` */
    data object TsNumber : TsTypeRef

    /** `boolean` */
    data object TsBoolean : TsTypeRef

    /** `unknown` — an explicitly opaque or unresolvable type. */
    data object TsUnknown : TsTypeRef

    /**
     * `null` — what `Unit` and `Nothing` actually put on the wire.
     *
     * Named for the JSON, not for Kotlin: `NullCodec` writes `null`, so the TypeScript type is `null`
     * and the schema is `z.null()`. Calling it `void` would describe the Kotlin side and mislead.
     */
    data object TsNull : TsTypeRef

    /** Wraps this reference as nullable, collapsing a double wrap. */
    fun asNullable(): TsTypeRef = when (this) {
        is Nullable -> this
        else -> Nullable(this)
    }

    /** Every [Named] id reachable from this reference, including nested ones. */
    fun referencedIds(): List<TypeId> = when (this) {
        is Named -> listOf(id) + args.flatMap { it.referencedIds() }
        is ArrayOf -> item.referencedIds()
        is RecordOf -> value.referencedIds()
        is Nullable -> inner.referencedIds()
        else -> emptyList()
    }
}
