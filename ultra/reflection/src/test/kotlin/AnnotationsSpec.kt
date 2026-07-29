package io.peekandpoke.ultra.reflection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MyMarker

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MetaAnnotation

@MetaAnnotation
@Target(AnnotationTarget.CLASS)
annotation class AnnotatedAnnotation

@MyMarker
open class AnnotatedBase {
    @MyMarker
    open val prop: String = "base"
}

class AnnotatedChild : AnnotatedBase() {
    override val prop: String = "child"
}

@AnnotatedAnnotation
class MetaAnnotated

class NotAnnotated

class AnnotationsSpec : StringSpec() {

    init {

        "findAnnotationsRecursive finds direct annotations" {
            val annotations = AnnotatedBase::class.findAnnotationsRecursive { it is MyMarker }

            annotations shouldBe listOf(AnnotatedBase::class.annotations.first { it is MyMarker })
        }

        "findAnnotationsRecursive returns empty for unannotated class" {
            val annotations = NotAnnotated::class.findAnnotationsRecursive { it is MyMarker }

            annotations.shouldBeEmpty()
        }

        "hasAnyAnnotationRecursive returns true when annotation present" {
            AnnotatedBase::class.hasAnyAnnotationRecursive { it is MyMarker } shouldBe true
        }

        "hasAnyAnnotationRecursive returns false when annotation absent" {
            NotAnnotated::class.hasAnyAnnotationRecursive { it is MyMarker } shouldBe false
        }

        "hasAnyAnnotationOnPropertyDefinedOnSuperTypes finds annotation on supertype property" {
            val childProp = AnnotatedChild::class.members.first { it.name == "prop" } as KProperty<*>

            childProp.hasAnyAnnotationOnPropertyDefinedOnSuperTypes(
                AnnotatedChild::class
            ) { it is MyMarker } shouldBe true
        }

        "hasAnyAnnotationOnPropertyDefinedOnSuperTypes returns false when no supertype annotation" {
            val childProp = AnnotatedChild::class.members.first { it.name == "prop" } as KProperty<*>

            childProp.hasAnyAnnotationOnPropertyDefinedOnSuperTypes(
                AnnotatedChild::class
            ) { it is MetaAnnotation } shouldBe false
        }

        // The meta-annotation walk is the only thing that makes these more than a plain filter
        // over `annotations`, and it had no coverage at all.

        "findAnnotationsRecursive finds an annotation on an annotation" {
            // MetaAnnotated -> @AnnotatedAnnotation -> @MetaAnnotation
            val annotations = MetaAnnotated::class.findAnnotationsRecursive { it is MetaAnnotation }

            annotations shouldHaveSize 1
            (annotations.first() is MetaAnnotation) shouldBe true
        }

        "findAnnotationsRecursive still finds the direct annotation on a meta-annotated class" {
            val annotations = MetaAnnotated::class.findAnnotationsRecursive { it is AnnotatedAnnotation }

            annotations shouldHaveSize 1
        }

        "hasAnyAnnotationRecursive sees through a meta-annotation" {
            MetaAnnotated::class.hasAnyAnnotationRecursive { it is MetaAnnotation } shouldBe true
        }

        "hasAnyAnnotationRecursive does not see a meta-annotation that is not there" {
            AnnotatedBase::class.hasAnyAnnotationRecursive { it is MetaAnnotation } shouldBe false
        }
    }
}
