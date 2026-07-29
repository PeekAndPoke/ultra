package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel

/**
 * Orders declarations for emission and works out which of them are recursive.
 *
 * Both outputs matter for zod, because a zod schema is a `const` — a VALUE, not a hoisted type:
 *
 * - **Order**: a schema referencing another must be declared after it, or the reference hits the
 *   temporal dead zone at module-evaluation time.
 * - **Recursion**: a genuine cycle cannot be ordered away. Those schemas are emitted as
 *   `z.lazy(() => ...)`, which defers the reference until first use. `z.infer` cannot see through
 *   `z.lazy`, so recursive types additionally need their TypeScript type written out explicitly
 *   instead of inferred.
 */
class TsDeclOrder private constructor(
    /** Declarations in a safe emission order. */
    val ordered: List<TsTypeDecl>,
    /** Ids that sit on a dependency cycle and therefore need `z.lazy` + an explicit type. */
    val recursive: Set<TypeId>,
) {
    companion object {

        private enum class Mark { VISITING, DONE }

        /**
         * Computes the emission order for [model], then marks every declaration that still points
         * forward.
         *
         * The DFS post-order alone is not enough. It yields a valid topological order for the acyclic
         * part, but a cycle has to break *somewhere*, and the declaration that needs deferring is the
         * one emitted FIRST while referencing something later — not the one the back-edge happens to
         * land on. So laziness is decided by position after ordering: a declaration is lazy when it
         * references itself, or anything at or after its own index.
         */
        fun of(model: TypeModel): TsDeclOrder {
            val marks = mutableMapOf<TypeId, Mark>()
            val ordered = mutableListOf<TsTypeDecl>()

            fun visit(id: TypeId) {
                if (marks[id] != null) return

                val decl = model.decls[id] ?: return

                marks[id] = Mark.VISITING

                decl.referencedIds().forEach { visit(it) }

                marks[id] = Mark.DONE
                ordered.add(decl)
            }

            model.decls.keys.forEach { visit(it) }

            val position = ordered.withIndex().associate { (idx, decl) -> decl.id to idx }

            val recursive = ordered
                .filter { decl ->
                    val self = position.getValue(decl.id)

                    decl.referencedIds().any { ref ->
                        // A reference to a claimed or undeclared type is an import, never a
                        // forward reference into this file.
                        val target = position[ref] ?: return@any false

                        target >= self
                    }
                }
                .map { it.id }
                .toSet()

            return TsDeclOrder(ordered = ordered, recursive = recursive)
        }
    }

    /** True when [id] must be emitted lazily. */
    fun isRecursive(id: TypeId): Boolean = id in recursive
}
