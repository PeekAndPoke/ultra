package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.kontainer.e2e.MyServiceInterface
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TypeLookupForSuperTypesSpec : StringSpec({

    open class MyServiceImplOne : MyServiceInterface

    class MyServiceImplTwo : MyServiceInterface

    class MyServiceDerivedFromImplOne : MyServiceImplOne()

    class SomeOtherService

    "Failing to find a super type" {
        val subject = TypeLookup.ForSuperTypes(setOf())

        assertSoftly {
            shouldThrow<ServiceNotFound> {
                subject.getDistinctFor(MyServiceInterface::class)
            }

            subject.getAllCandidatesFor(MyServiceInterface::class) shouldBe setOf()
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

            subject.getDistinctFor(MyServiceInterface::class) shouldBe MyServiceImplOne::class

            subject.getAllCandidatesFor(MyServiceInterface::class) shouldBe setOf(MyServiceImplOne::class)
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
                subject.getDistinctFor(MyServiceInterface::class)
            }

            subject.getAllCandidatesFor(MyServiceInterface::class) shouldBe setOf(
                MyServiceImplOne::class,
                MyServiceImplTwo::class,
                MyServiceDerivedFromImplOne::class
            )
        }
    }
})
