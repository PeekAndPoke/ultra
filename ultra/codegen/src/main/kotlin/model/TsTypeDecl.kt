package io.peekandpoke.ultra.codegen.model

/** A property of a generated object type. */
data class TsProp(
    /** The wire name — exactly the key Slumber writes, never a sanitized variant. */
    val name: String,
    val type: TsTypeRef,
    /** True when the Kotlin constructor parameter has a default, i.e. the key may be absent. */
    val optional: Boolean = false,
)

/**
 * The discriminator a polymorphic child writes into its JSON.
 *
 * [field] comes from `PolymorphicParentUtil.getDiscriminator` — it is NOT always `_type`, since a
 * parent companion implementing `Polymorphic.Parent` can override it.
 */
data class TsDiscriminator(
    val field: String,
    val literal: String,
)

/** A generated TypeScript declaration. */
sealed interface TsTypeDecl {
    val id: TypeId

    /** The generated TypeScript name. Unique across the model — enforced during validation. */
    val name: String

    /** An object type: a Kotlin data class, or one variant of a polymorphic hierarchy. */
    data class Obj(
        override val id: TypeId,
        override val name: String,
        val props: List<TsProp>,
        /** Set when this object is a polymorphic child, so the emitter adds the literal field. */
        val discriminator: TsDiscriminator? = null,
    ) : TsTypeDecl

    /** A polymorphic parent: emitted as a discriminated union over [variants]. */
    data class Union(
        override val id: TypeId,
        override val name: String,
        val discriminatorField: String,
        val variants: List<TypeId>,
    ) : TsTypeDecl

    /** An enum: emitted as a union of string literals over the constant names. */
    data class EnumDecl(
        override val id: TypeId,
        override val name: String,
        val values: List<String>,
    ) : TsTypeDecl

    /**
     * A `@JvmInline value class`: emitted as an alias to its underlying type.
     *
     * `ValueClassSlumberer` writes the bare underlying value, never a wrapper object.
     */
    data class Alias(
        override val id: TypeId,
        override val name: String,
        val target: TsTypeRef,
    ) : TsTypeDecl

    /** Every type id this declaration references. */
    fun referencedIds(): List<TypeId> = when (this) {
        is Obj -> props.flatMap { it.type.referencedIds() }
        is Union -> variants
        is EnumDecl -> emptyList()
        is Alias -> target.referencedIds()
    }
}
