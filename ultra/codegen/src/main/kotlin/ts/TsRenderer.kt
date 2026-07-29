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

    /** Renders [ref] as a TypeScript type expression. */
    fun type(ref: TsTypeRef): String = when (ref) {
        is TsTypeRef.Named -> nameOf(ref.id)

        // A union member needs parentheses before `[]` binds: `(A | null)[]`, not `A | null[]`.
        is TsTypeRef.ArrayOf -> type(ref.item).let { inner ->
            if (needsParensBeforeArray(ref.item)) "($inner)[]" else "$inner[]"
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
        is TsTypeRef.Named -> schemaNameOf(ref.id)

        is TsTypeRef.ArrayOf -> "z.array(${schema(ref.item)})"

        is TsTypeRef.RecordOf -> "z.record(z.string(), ${schema(ref.value)})"

        is TsTypeRef.Nullable -> "${schema(ref.inner)}.nullable()"

        TsTypeRef.TsString -> "z.string()"
        TsTypeRef.TsNumber -> "z.number()"
        TsTypeRef.TsBoolean -> "z.boolean()"
        TsTypeRef.TsUnknown -> "z.unknown()"
        TsTypeRef.TsNull -> "z.null()"
    }

    /** True when [ref] renders as a union, which must be parenthesised before an array suffix. */
    private fun needsParensBeforeArray(ref: TsTypeRef): Boolean = ref is TsTypeRef.Nullable
}
