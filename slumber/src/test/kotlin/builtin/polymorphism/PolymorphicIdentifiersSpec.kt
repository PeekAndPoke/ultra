package de.peekandpoke.ultra.slumber.builtin.polymorphism

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PolymorphicIdentifiersSpec : FreeSpec() {

    init {
        "Get all identifiers to child classes" {
            val map = PolymorphicParentUtil.getIdentifiersToChildClasses(ChildrenUsingAnnotation::class)

            map shouldBe mapOf(
                "Sub.Deeper1" to ChildrenUsingAnnotation.Sub.Deeper1::class,
                "Sub.Deeper2" to ChildrenUsingAnnotation.Sub.Deeper2::class,
                "Sub.Deeper2.Additional" to ChildrenUsingAnnotation.Sub.Deeper2::class,
                "Sub2" to ChildrenUsingAnnotation.Sub2::class,
                "Sub3" to ChildrenUsingAnnotation.Sub3::class,
                "Sub3-Additional" to ChildrenUsingAnnotation.Sub3::class,
                "Sub3-Additional-2" to ChildrenUsingAnnotation.Sub3::class,
            )
        }

        "Get all identifiers using @SerialName only" {

            val identifiers = PolymorphicChildUtil.getAllIdentifiers(ChildrenUsingAnnotation.Sub2::class)

            identifiers shouldBe listOf(
                "Sub2" to ChildrenUsingAnnotation.Sub2::class,
            )
        }

        "Get all identifiers using @SerialName and @AdditionalSerialName" {

            val identifiers = PolymorphicChildUtil.getAllIdentifiers(ChildrenUsingAnnotation.Sub3::class)

            identifiers shouldBe listOf(
                "Sub3" to ChildrenUsingAnnotation.Sub3::class,
                "Sub3-Additional" to ChildrenUsingAnnotation.Sub3::class,
                "Sub3-Additional-2" to ChildrenUsingAnnotation.Sub3::class,
            )
        }
    }
}
