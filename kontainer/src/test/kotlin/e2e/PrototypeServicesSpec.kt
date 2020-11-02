package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class PrototypeServicesSpec : StringSpec({

    "Retrieving a prototype multiple times" {

        class MyPrototype

        val subject = kontainer {
            prototype(MyPrototype::class)
        }.useWith()

        val first = subject.get(MyPrototype::class)
        val second = subject.get(MyPrototype::class)

        assertSoftly {
            first shouldNotBeSameInstanceAs second

            subject.getProvider(MyPrototype::class).type shouldBe ServiceProvider.Type.Prototype
        }
    }

    "Injecting a prototype into a singleton service" {

        class MyPrototype
        class Injecting(val proto: MyPrototype)

        val subject = kontainer {
            prototype<MyPrototype>()
            singleton(Injecting::class)
        }.useWith()

        val first = subject.get(Injecting::class)
        val second = subject.get(Injecting::class)

        val prototype = subject.get(MyPrototype::class)

        assertSoftly {
            first shouldBeSameInstanceAs second
            first.proto shouldBeSameInstanceAs second.proto

            first.proto shouldNotBeSameInstanceAs prototype

            subject.getProvider(Injecting::class).type shouldBe ServiceProvider.Type.Singleton
            subject.getProvider(MyPrototype::class).type shouldBe ServiceProvider.Type.Prototype
        }
    }

    "Registering a prototype service with explicit base type" {

        abstract class MyServiceBase

        abstract class MyService : MyServiceBase()

        class MyServiceImpl : MyService()

        val subject = kontainer {
            prototype(MyService::class, MyServiceImpl::class)
        }.useWith()

        assertSoftly {

            subject.get(MyService::class)::class shouldBe MyServiceImpl::class

            subject.get(MyServiceBase::class)::class shouldBe MyServiceImpl::class
        }
    }
})
