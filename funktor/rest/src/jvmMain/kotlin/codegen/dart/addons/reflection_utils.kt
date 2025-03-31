package de.peekandpoke.funktor.rest.codegen.dart.addons

import de.peekandpoke.ultra.slumber.Polymorphic
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

fun <T : Any> KClass<T>.getDirectPolymorphicChildren(): Set<KClass<*>> {
    val sealed = sealedSubclasses

    val companion = (companionObjectInstance as? Polymorphic.Parent)?.childTypes ?: emptySet()

    return sealed.plus(companion).toSet()
}

