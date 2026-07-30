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

        /**
         * The identity of a DECLARATION: the class alone, ignoring its type arguments.
         *
         * `PageOf<Talk>` and `PageOf<Speaker>` share one declaration, so they must share one id. The
         * arguments live on the [TsTypeRef.Named] that points here, not on the declaration.
         *
         * [representative] is a real reified instantiation, kept as [type] purely so validation can
         * still probe a concrete `KType` — `SlumberConfig.getSlumberer` needs one, and Slumber
         * dispatches custom codecs on the classifier, so any instantiation answers the question. Ids
         * compare by [key], so whichever instantiation is seen first wins and equality is unaffected.
         */
        fun declOf(cls: KClass<*>, representative: KType): TypeId =
            TypeId(type = representative, key = cls.qualifiedName ?: cls.jvmName)

        private fun canonicalKey(type: KType): String {
            val cls = type.classifier as? KClass<*>
                ?: return type.toString()

            val base = cls.qualifiedName ?: cls.jvmName

            val args = type.arguments.mapNotNull { it.type }

            return when {
                args.isEmpty() -> base

                // A type ARGUMENT's nullability is part of the declaration's content, unlike the
                // top-level nullability stripped above: `Box<String>` and `Box<String?>` reify to
                // different props, so collapsing them onto one key would silently give the second one
                // the first one's schema.
                else -> args.joinToString(prefix = "$base<", postfix = ">") { arg ->
                    canonicalKey(arg) + if (arg.isMarkedNullable) "?" else ""
                }
            }
        }
    }

    /** The raw class behind this id. */
    val cls: KClass<*> get() = type.classifier as KClass<*>

    /** Simple (unqualified) name, used as the basis for the generated TypeScript name. */
    val simpleName: String get() = cls.simpleName ?: cls.jvmName.substringAfterLast('.')

    override fun equals(other: Any?): Boolean = this === other || (other is TypeId && other.key == key)

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key
}
