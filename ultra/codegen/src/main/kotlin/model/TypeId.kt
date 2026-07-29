package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName

/**
 * Identity of a type in the generated model — a fully reified [KType] with its nullability stripped.
 *
 * Equality is by [key], a canonical string built from qualified names, NOT by [KType] equality.
 * The same logical type reaches the walker as different [KType] instances depending on whether it
 * came from `typeOf<T>()` or from `KClass.createType(...)` during reification, and those do not
 * reliably compare equal. Keying by a canonical string is the same idea the old Dart `Tags` used
 * (comparing `qualifiedName`), made total.
 *
 * Nullability lives on a [TsTypeRef], not here: `Talk` and `Talk?` share one declaration.
 */
class TypeId private constructor(
    /** The reified, non-null type this id stands for. */
    val type: KType,
    /** Canonical identity string, e.g. `com.acme.PageOf<com.acme.Talk>`. */
    val key: String,
) {
    companion object {
        /** Creates the id for [type], stripping nullability and canonicalizing type arguments. */
        fun of(type: KType): TypeId = TypeId(type = type, key = canonicalKey(type))

        /** Creates the id for a raw [cls] with no type arguments. */
        fun of(cls: KClass<*>): TypeId = of(cls.createBareType())

        private fun canonicalKey(type: KType): String {
            val cls = type.classifier as? KClass<*>
                ?: return type.toString()

            val base = cls.qualifiedName ?: cls.jvmName

            val args = type.arguments.mapNotNull { it.type }

            return when {
                args.isEmpty() -> base
                else -> args.joinToString(prefix = "$base<", postfix = ">") { canonicalKey(it) }
            }
        }
    }

    /** The raw class behind this id. */
    val cls: KClass<*> get() = type.classifier as KClass<*>

    /** Simple (unqualified) name, used as the basis for the generated TypeScript name. */
    val simpleName: String get() = cls.simpleName ?: cls.jvmName.substringAfterLast('.')

    /** The reified type arguments, empty for a non-generic type. */
    val typeArguments: List<TypeId> get() = type.arguments.mapNotNull { arg -> arg.type?.let { of(it) } }

    override fun equals(other: Any?): Boolean = this === other || (other is TypeId && other.key == key)

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key
}
