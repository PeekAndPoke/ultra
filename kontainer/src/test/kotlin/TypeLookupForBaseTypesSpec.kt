package de.peekandpoke.ultra.kontainer

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TypeLookupForBaseTypesSpec : StringSpec({

    open class MyServiceImplOne : MyService

    class SomeOtherService

    "Failing to find a base type" {

        val subject = TypeLookup.ForBaseTypes(setOf())

        assertSoftly {
            shouldThrow<ServiceNotFound> {
                subject.getDistinctFor(MyService::class)
            }

            subject.getAllCandidatesFor(MyService::class) shouldBe setOf()
        }
    }

    "Finding a type by its own type" {

        val subject = TypeLookup.ForBaseTypes(
            setOf(
                MyServiceImplOne::class,
                SomeOtherService::class
            )
        )

        assertSoftly {
            subject.getDistinctFor(MyServiceImplOne::class) shouldBe MyServiceImplOne::class

            subject.getDistinctFor(SomeOtherService::class) shouldBe SomeOtherService::class
        }
    }

    "Finding a base type by its super type" {

        val subject = TypeLookup.ForBaseTypes(
            setOf(
                MyService::class,
                SomeOtherService::class
            )
        )

        assertSoftly {
            subject.getDistinctFor(MyServiceImplOne::class) shouldBe MyService::class

            subject.getDistinctFor(SomeOtherService::class) shouldBe SomeOtherService::class
        }
    }
})
