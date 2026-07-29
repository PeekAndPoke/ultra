package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Derives TypeScript names from Kotlin types.
 *
 * Generic types are monomorphized: `PageOf<Talk>` becomes `PageOfTalk`, not a generic `PageOf<T>`.
 * See the task doc for the rationale — in short, the walker reifies type arguments anyway (so
 * monomorphizing is free while staying generic would mean un-reifying), and generic zod schemas
 * require function-valued schemas that `z.infer` cannot see through.
 */
object TsNames {

    /**
     * The TypeScript name for [id].
     *
     * Nested classes keep their outer names (`Foo.Bar` -> `FooBar`) so two nested classes with the
     * same simple name in one package do not collide.
     */
    fun of(id: TypeId): String {
        val base = baseName(id.cls)

        val args = id.typeArguments

        return when {
            args.isEmpty() -> base
            else -> base + args.joinToString("") { of(it) }
        }
    }

    /** The unqualified name of [cls] with any outer-class prefixes retained and dots removed. */
    private fun baseName(cls: KClass<*>): String {
        val qualified = cls.qualifiedName ?: return cls.jvmName.substringAfterLast('.').replace('$', '_')

        val packageName = cls.java.`package`?.name ?: ""

        val withoutPackage = when {
            packageName.isNotEmpty() && qualified.startsWith("$packageName.") ->
                qualified.removePrefix("$packageName.")

            else -> qualified
        }

        return withoutPackage.replace(".", "")
    }
}
