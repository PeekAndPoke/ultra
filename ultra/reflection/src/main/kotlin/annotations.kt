package io.peekandpoke.ultra.reflection

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties

/**
 * Recursively finds all annotations on this element that match the given [predicate].
 *
 * Meta-annotations (annotations on annotations) are included in the search,
 * so an annotation applied indirectly through a meta-annotated annotation will be found.
 *
 * Each annotation appears once, no matter how many paths reach it.
 */
fun KAnnotatedElement.findAnnotationsRecursive(
    predicate: (Annotation) -> Boolean,
): List<Annotation> {
    // One traversal across all direct annotations, sharing a single visited set. Walking each
    // direct annotation separately would report a shared meta-annotation once per path.
    val all = mutableSetOf<Annotation>()

    fun visit(annotation: Annotation) {
        if (all.add(annotation)) {
            annotation.annotationClass.annotations.forEach { visit(it) }
        }
    }

    annotations.forEach { visit(it) }

    return all.filter { an -> predicate(an) }
}

/**
 * Returns `true` if this element has any annotation (including meta-annotations) matching the [predicate].
 *
 * @see findAnnotationsRecursive
 */
fun KAnnotatedElement.hasAnyAnnotationRecursive(
    predicate: (Annotation) -> Boolean,
): Boolean {
    return findAnnotationsRecursive(predicate).isNotEmpty()
}

/**
 * Returns `true` if any super-type of [cls] declares a property with the same name as this property
 * and that property has an annotation matching the [predicate].
 *
 * This is useful when a child class overrides a property without re-declaring annotations,
 * and you need to check whether the original definition in a super-type was annotated.
 *
 * @see hasAnyAnnotationRecursive
 */
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
