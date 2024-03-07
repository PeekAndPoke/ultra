package de.peekandpoke.ultra.common.reflection

import de.peekandpoke.ultra.common.recursion.flattenTreeToSet
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties

fun KAnnotatedElement.findAnnotationsRecursive(
    predicate: (Annotation) -> Boolean,
): List<Annotation> {
    val all = annotations.flatMap { an ->
        an.flattenTreeToSet {
            it.annotationClass.annotations
        }
    }

    return all.filter { an -> predicate(an) }
}

fun KAnnotatedElement.hasAnyAnnotationRecursive(
    predicate: (Annotation) -> Boolean,
): Boolean {
    return findAnnotationsRecursive(predicate).isNotEmpty()
}

fun KProperty<*>.hasAnyAnnotationOnPropertyDefinedOnSuperTypes(
    cls: KClass<*>,
    predicate: (Annotation) -> Boolean,
): Boolean {
    return cls.allSuperclasses.any { superClass ->
        superClass.declaredMemberProperties
            .filter { it.name == name }
            .any { it.hasAnyAnnotationRecursive(predicate) }
    }
}
