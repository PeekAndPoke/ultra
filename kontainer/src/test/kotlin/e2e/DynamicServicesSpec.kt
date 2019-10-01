package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class DynamicServicesSpec : StringSpec({

    "Missing a dynamic service when creating the container" {

        val blueprint = kontainer {
            dynamic<MyService>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain "Some dynamics were not provided: "
            error.message shouldContain MyService::class.qualifiedName!!
        }
    }

    "Providing an unexpected instance when creating the container" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class)
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith(
                    SimpleService(),
                    DynamicService(100)
                )
            }

            error.message shouldContain "Unexpected dynamics were provided: "
            error.message shouldContain SimpleService::class.qualifiedName!!
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

    "Providing a dynamic service and getting it multiple times from the SAME container" {

        val blueprint = kontainer {
            dynamic<SimpleService>()
        }

        val subject = blueprint.useWith(
            SimpleService()
        )

        val first = subject.get<SimpleService>()
        val second = subject.get<SimpleService>()

        assertSoftly {

            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Dynamic

            first::class shouldBe SimpleService::class
            first.get() shouldBe 0

            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service and getting it multiple times from DIFFERENT container" {

        val blueprint = kontainer {
            dynamic<SimpleService>()
        }

        val containerOne = blueprint.useWith(
            SimpleService()
        )

        val first = containerOne.get<SimpleService>()

        val containerTwo = blueprint.useWith(
            SimpleService()
        )

        val second = containerTwo.get<SimpleService>()

        assertSoftly {
            first shouldNotBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic0 { DynamicService(100) }
        }

        val subject = blueprint.useWith()

        assertSoftly {

            subject.get<DynamicService>()::class shouldBe DynamicService::class
            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.DynamicDefault

            subject.get<DynamicService>().value shouldBe 100
        }
    }

    "Providing a dynamic service with default and getting it multiple times from the SAME containers" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic0 { DynamicService(100) }
        }

        val subject = blueprint.useWith()

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {
            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and getting it multiple times from DIFFERENT containers" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic0 { DynamicService(100) }
        }

        val subjectOne = blueprint.useWith()
        val first = subjectOne.get<DynamicService>()

        val subjectTwo = blueprint.useWith()
        val second = subjectTwo.get<DynamicService>()

        assertSoftly {
            first shouldNotBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and overriding it in useWith()" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic0 { DynamicService(100) }
        }

        val subject = blueprint.useWith(DynamicService(200))

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {

            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.Dynamic

            first::class shouldBe DynamicService::class
            first.value shouldBe 200

            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and overriding it with a super type when creating the container" {

        open class DynamicService(val value: Int)
        class DerivedService(value: Int) : DynamicService(value)

        val blueprint = kontainer {
            dynamic0 { DynamicService(100) }
        }

        val subject = blueprint.useWith(DerivedService(200))

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {

            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.Dynamic

            first::class shouldBe DerivedService::class
            first.value shouldBe 200

            first shouldBeSameInstanceAs second
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

            subject.getProvider<SomeIndependentService>().type shouldBe ServiceProvider.Type.Singleton

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

    "Singletons do not become semi-dynamic when injecting non dynamic services lazily" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Lazy<Base>)

        val subject = kontainer {

            singleton<Impl>()
            singleton<Injecting>()

        }.useWith()

        assertSoftly {

            subject.getProvider<Impl>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Singletons become semi-dynamic when injecting a list, where at least one in the list is dynamic" {

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

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons do not become semi-dynamic when injecting a list, where none in the list is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: List<Base>)

        val subject = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            singleton<ImplTwo>()

        }.useWith()

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Singletons become semi-dynamic when lazily injecting a list, where at least one in the list is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lazy<List<Base>>)

        val subject = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            dynamic<ImplTwo>()

        }.useWith(ImplTwo())

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Dynamic

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons do not become semi-dynamic when lazily injecting a list, where none in the list is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lazy<List<Base>>)

        val subject = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            singleton<ImplTwo>()

        }.useWith()

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Singletons become semi-dynamic when injecting a lookup, where at least one in the lookup is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lookup<Base>)

        val blueprint = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            dynamic<ImplTwo>()

        }

        val subjectOne = blueprint.useWith(ImplTwo())
        val first = subjectOne.get<Injecting>()

        val subjectTwo = blueprint.useWith(ImplTwo())
        val second = subjectTwo.get<Injecting>()

        assertSoftly {

            subjectOne.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton
            // ImplOne must be the same instance in both containers as it is a GlobalSingleton
            first.all.get(ImplOne::class) shouldBeSameInstanceAs second.all.get(ImplOne::class)

            subjectOne.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Dynamic
            // ImplTwo must NOT be the same instance in both containers as it is a Dynamic
            first.all.get(ImplTwo::class) shouldNotBeSameInstanceAs second.all.get(ImplTwo::class)

            subjectOne.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
            // Injecting must NOT be the same instance in both containers as it is a SemiDynamic
            first shouldNotBeSameInstanceAs second
        }
    }

    "Singletons do not become semi-dynamic when injecting a lookup, where none in the lookup is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lookup<Base>)

        val subject = kontainer {

            singleton<Injecting>()

            singleton<ImplOne>()
            singleton<ImplTwo>()

        }.useWith()

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.Singleton
        }
    }
})
