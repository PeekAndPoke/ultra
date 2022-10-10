package de.peekandpoke.ultra.common.reflection

import de.peekandpoke.ultra.common.recursion.flattenTreeToSet
import kotlin.reflect.KAnnotatedElement

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
