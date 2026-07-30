package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel

/**
 * Renders [TsTypeRef]s as TypeScript type expressions and as zod schema expressions.
 *
 * Both directions are needed: non-recursive declarations derive their type via `z.infer`, but
 * recursive ones must spell the type out (see [TsDeclOrder]).
 */
class TsRenderer(private val model: TypeModel) {

    /** The TypeScript name for [id] — from its declaration, or from the claim that owns it. */
    fun nameOf(id: TypeId): String =
        model.decls[id]?.name
            ?: model.usedClaims[id.cls.qualifiedName]?.tsName
            ?: "unknown"

    /** The zod schema expression for [id]. */
    fun schemaNameOf(id: TypeId): String =
        model.decls[id]?.name
            ?: model.usedClaims[id.cls.qualifiedName]?.schema
            ?: "z.unknown()"

    /**
     * The identifier a generic declaration takes its schema argument under.
     *
     * Suffixed rather than case-mangled so it is injective: parameter names are unique within a
     * declaration, so appending a fixed suffix cannot make two of them collide. Lower-casing would —
     * `T` and `t` both become `t`.
     */
    fun schemaParamOf(typeParam: String): String = "${typeParam}Schema"

    /** Renders [ref] as a TypeScript type expression. */
    fun type(ref: TsTypeRef): String = when (ref) {
        is TsTypeRef.Named -> nameOf(ref.id) + ref.args.render { type(it) }

        is TsTypeRef.TypeParam -> ref.name

        // A union member needs parentheses before `[]` binds: `(A | null)[]`, not `A | null[]`,
        // which would parse as `A | (null[])` — a different type entirely.
        is TsTypeRef.ArrayOf -> type(ref.item).let { inner ->
            if (inner.isUnion()) "($inner)[]" else "$inner[]"
        }

        is TsTypeRef.RecordOf -> "Record<string, ${type(ref.value)}>"

        is TsTypeRef.Nullable -> "${type(ref.inner)} | null"

        TsTypeRef.TsString -> "string"
        TsTypeRef.TsNumber -> "number"
        TsTypeRef.TsBoolean -> "boolean"
        TsTypeRef.TsUnknown -> "unknown"
        TsTypeRef.TsNull -> "null"
    }

    /** Renders [ref] as a zod schema expression. */
    fun schema(ref: TsTypeRef): String = when (ref) {
        // A generic schema is a FACTORY, so an instantiation is a call rather than a name. Type
        // position stays a plain reference — see `type` above.
        is TsTypeRef.Named -> schemaNameOf(ref.id) + ref.args.renderCall { schema(it) }

        is TsTypeRef.TypeParam -> schemaParamOf(ref.name)

        is TsTypeRef.ArrayOf -> "z.array(${schema(ref.item)})"

        is TsTypeRef.RecordOf -> "z.record(z.string(), ${schema(ref.value)})"

        is TsTypeRef.Nullable -> "${schema(ref.inner)}.nullable()"

        TsTypeRef.TsString -> "z.string()"
        TsTypeRef.TsNumber -> "z.number()"
        TsTypeRef.TsBoolean -> "z.boolean()"
        TsTypeRef.TsUnknown -> "z.unknown()"
        TsTypeRef.TsNull -> "z.null()"
    }

    /**
     * True when a rendered type is a union and so must be parenthesised before an array suffix.
     *
     * Checks the rendered TEXT rather than the [TsTypeRef] shape, because a CLAIMED type can also be a
     * union — `JsonPrimitive` is claimed as `string | number | boolean | null` — and no inspection of
     * the ref would reveal that. Over-parenthesising is harmless in TypeScript, so erring that way is
     * safe; under-parenthesising silently changes the type.
     */
    private fun String.isUnion(): Boolean = contains(" | ")

    /** `<A, B>`, or empty when there are no arguments. */
    private fun List<TsTypeRef>.render(each: (TsTypeRef) -> String): String =
        if (isEmpty()) "" else joinToString(", ", "<", ">") { each(it) }

    /** `(a, b)`, or empty when there are no arguments. */
    private fun List<TsTypeRef>.renderCall(each: (TsTypeRef) -> String): String =
        if (isEmpty()) "" else joinToString(", ", "(", ")") { each(it) }
}
