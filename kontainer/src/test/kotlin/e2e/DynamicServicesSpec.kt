package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class DynamicServicesSpec : StringSpec({

    "Missing a dynamic service with creating the container" {

        val blueprint = kontainer {
            dynamic<MyService>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain "Some mandatory dynamic services are missing"
            error.message shouldContain MyService::class.qualifiedName!!
        }
    }

    "Providing a dynamic service" {

        val blueprint = kontainer {
            dynamic<SimpleService>()
        }

        val subject = blueprint.useWith(
            SimpleService()
        )

        assertSoftly {

            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Dynamic

            subject.get<SimpleService>().get() shouldBe 0
        }
    }

    "Providing a dynamic service by super type" {

        val blueprint = kontainer {
            dynamic<SimpleService>()
        }

        val subject = blueprint.useWith(
            SuperSimpleService()
        )

        assertSoftly {

            subject.get<SimpleService>()::class shouldBe SuperSimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Dynamic

            subject.get<SimpleService>().get() shouldBe 0
        }
    }

    "Dynamic services must be re-created for each container instance" {

        val blueprint = kontainer {
            dynamic<SimpleService>()
        }

        val first = blueprint.useWith(
            SuperSimpleService()
        )
        first.get<SimpleService>().inc()

        val second = blueprint.useWith(
            SuperSimpleService()
        )
        second.get<SimpleService>().inc()

        assertSoftly {
            first.get<SimpleService>()::class shouldNotBe second.get<SimpleService>()

            first.get<SimpleService>().get() shouldBe 1

            second.get<SimpleService>().get() shouldBe 1
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services" {

        val blueprint = kontainer {
            // This one must stay a GlobalSingleton
            singleton<SomeIndependentService>()

            // this one injects InjectingService and has to become SemiDynamic
            singleton<DeeperInjectingService>()
            // this one injects InjectingService SimpleService and has to become SemiDynamic
            singleton<InjectingService>()

            // this one has to be Dynamic
            dynamic<SimpleService>()
            // this one has to be Dynamic
            dynamic<AnotherSimpleService>()
        }

        val subject = blueprint.useWith(
            SimpleService(),
            AnotherSimpleService()
        )

        assertSoftly {

            subject.getProvider<SomeIndependentService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.getProvider<DeeperInjectingService>().type shouldBe ServiceProvider.Type.SemiDynamic

            subject.getProvider<InjectingService>().type shouldBe ServiceProvider.Type.SemiDynamic

            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.Dynamic
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services by base type" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Base)

        val subject = kontainer {

            dynamic<Impl>()

            singleton<Injecting>()

        }.useWith(Impl())

        assertSoftly {

            subject.getProvider<Impl>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services lazily" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Lazy<Base>)

        val subject = kontainer {

            dynamic<Impl>()

            singleton<Injecting>()

        }.useWith(Impl())

        assertSoftly {

            subject.getProvider<Impl>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons become semi-dynamic when injecting all by base type, where at least one is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: List<Base>)

        val subject = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            dynamic<ImplTwo>()

        }.useWith(ImplTwo())

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }
})
