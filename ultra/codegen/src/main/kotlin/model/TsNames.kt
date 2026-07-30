package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Derives TypeScript names from Kotlin types.
 *
 * A name is the class's own name and nothing else.
 *
 * Generic types are emitted GENERICALLY, so type arguments live on the reference — `PageOf<Talk>` in
 * type position, `PageOf(Talk)` in schema position — rather than being baked into a monomorphized name
 * such as `PageOfTalk`. Two same-named classes in different packages therefore still collapse onto one
 * name; that is caught by the collision check in `TsModelValidator`.
 */
object TsNames {

    /**
     * The TypeScript name for [id].
     *
     * Nested classes keep their outer names (`Foo.Bar` -> `FooBar`) so two nested classes with the
     * same simple name in one package do not collide.
     */
    fun of(id: TypeId): String = baseName(id.cls)

    /**
     * The unqualified name of [cls], with outer-class prefixes retained.
     *
     * Built by walking the enclosing-class chain rather than by stripping the package off the
     * qualified name. Those two disagree for Kotlin's mapped types — `List::class.java` is
     * `java.util.List` while `qualifiedName` is `kotlin.collections.List`, so package stripping left
     * the whole thing intact and produced names like `FxBoxkotlincollectionsListFxSpeaker`.
     *
     * Two same-named classes in different packages still collapse to one name, exactly as before;
     * that is caught by the name-collision check in `TsModelValidator`.
     */
    private fun baseName(cls: KClass<*>): String {
        val parts = mutableListOf<String>()

        var current: Class<*>? = cls.java

        while (current != null) {
            val simple = current.simpleName

            // Anonymous and local classes have a blank simple name; fall back to the binary name.
            if (simple.isNullOrEmpty()) {
                return cls.jvmName.substringAfterLast('.').replace('$', '_')
            }

            parts.add(0, simple)
            current = current.enclosingClass
        }

        return parts.joinToString("")
    }
}
