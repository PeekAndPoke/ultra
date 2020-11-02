package de.peekandpoke.ultra.kontainer

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TypeLookupForSuperTypesSpec : StringSpec({

    open class MyServiceImplOne : MyService

    class MyServiceImplTwo : MyService

    class MyServiceDerivedFromImplOne : MyServiceImplOne()

    class SomeOtherService

    "Failing to find a super type" {
        val subject = TypeLookup.ForSuperTypes(setOf())

        assertSoftly {
            shouldThrow<ServiceNotFound> {
                subject.getDistinctFor(MyService::class)
            }

            subject.getAllCandidatesFor(MyService::class) shouldBe setOf()
        }
    }

    "Finding a type by its own type" {

        val subject = TypeLookup.ForSuperTypes(
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

    "Finding a type by its base type" {

        val subject = TypeLookup.ForSuperTypes(
            setOf(
                MyServiceImplOne::class,
                SomeOtherService::class
            )
        )

        assertSoftly {

            subject.getDistinctFor(MyService::class) shouldBe MyServiceImplOne::class

            subject.getAllCandidatesFor(MyService::class) shouldBe setOf(MyServiceImplOne::class)
        }
    }

    "Finding multiple candidates by base type" {

        val subject = TypeLookup.ForSuperTypes(
            setOf(
                MyServiceImplOne::class,
                MyServiceImplTwo::class,
                MyServiceDerivedFromImplOne::class
            )
        )

        assertSoftly {

            shouldThrow<ServiceAmbiguous> {
                subject.getDistinctFor(MyService::class)
            }

            subject.getAllCandidatesFor(MyService::class) shouldBe setOf(
                MyServiceImplOne::class,
                MyServiceImplTwo::class,
                MyServiceDerivedFromImplOne::class
            )
        }
    }
})
