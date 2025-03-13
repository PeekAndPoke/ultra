package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.kontainer.InvalidClassProvided
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class DynamicServicesSpec : StringSpec({

    "Defining a dynamic service by interface without default must fail" {

        assertSoftly {

            val error = shouldThrow<InvalidClassProvided> {
                kontainer {
                    dynamic(MyServiceInterface::class)
                }
            }

            error.message shouldContain "A service cannot be an interface or abstract class"
        }
    }

    "Providing an unexpected instance when creating the container" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class) {
                DynamicService(10)
            }
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create {
                    with { CounterService() }
                    with { DynamicService(100) }
                }
            }

            error.message shouldContain "Unexpected dynamics were provided: "
            error.message shouldContain CounterService::class.qualifiedName!!
        }
    }

    "Providing a dynamic service when creating he kontainer" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val subject = blueprint.create {
            with { CounterService() }
        }

        assertSoftly {

            subject.get<CounterService>()::class shouldBe CounterService::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.get<CounterService>().get() shouldBe 0
        }
    }

    "Providing a dynamic service with base class and impl class" {

        abstract class MyBase
        class MyImpl : MyBase()

        val blueprint = kontainer {
            dynamic(MyBase::class, MyImpl::class)
        }

        val subject = blueprint.create()

        assertSoftly {

            shouldThrow<ServiceNotFound> {
                subject.get(MyImpl::class)
            }

            subject.get<MyBase>()::class shouldBe MyImpl::class
        }
    }

    "Providing a dynamic service and getting it multiple times from the SAME container" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val subject = blueprint.create {
            with { CounterService() }
        }

        val first = subject.get<CounterService>()
        val second = subject.get<CounterService>()

        assertSoftly {

            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.DynamicOverride

            first::class shouldBe CounterService::class
            first.get() shouldBe 0

            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service and getting it multiple times from DIFFERENT container" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val containerOne = blueprint.create {
            with { CounterService() }
        }

        val first = containerOne.get<CounterService>()

        val containerTwo = blueprint.create {
            with { CounterService() }
        }

        val second = containerTwo.get<CounterService>()

        assertSoftly {
            first shouldNotBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subject = blueprint.create()

        assertSoftly {

            subject.get<DynamicService>()::class shouldBe DynamicService::class
            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.Dynamic

            subject.get<DynamicService>().value shouldBe 100
        }
    }

    "Providing a dynamic service with default and getting it multiple times from the SAME containers" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subject = blueprint.create()

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {
            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and getting it multiple times from DIFFERENT containers" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subjectOne = blueprint.create()
        val first = subjectOne.get<DynamicService>()

        val subjectTwo = blueprint.create()
        val second = subjectTwo.get<DynamicService>()

        assertSoftly {
            first shouldNotBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and overriding it in usewith {  }" {

        data class DynamicService(val value: Int)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subject = blueprint.create {
            with { DynamicService(200) }
        }

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {

            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.DynamicOverride

            first::class shouldBe DynamicService::class
            first.value shouldBe 200

            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and overriding it with a super type when creating the container" {

        open class DynamicService(val value: Int)
        class DerivedService(value: Int) : DynamicService(value)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subject = blueprint.create {
            with { DerivedService(200) }
        }

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {

            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.DynamicOverride

            first::class shouldBe DerivedService::class
            first.value shouldBe 200

            first shouldBeSameInstanceAs second
        }
    }

    "Providing a dynamic service with default and overriding it with by its base type" {

        open class DynamicService(val value: Int)
        class DerivedService(value: Int) : DynamicService(value)

        val blueprint = kontainer {
            dynamic(DynamicService::class) { DynamicService(100) }
        }

        val subject = blueprint.create {
            with(DynamicService::class) { DerivedService(200) }
        }

        val first = subject.get<DynamicService>()
        val second = subject.get<DynamicService>()

        assertSoftly {

            subject.getProvider<DynamicService>().type shouldBe ServiceProvider.Type.DynamicOverride

            first::class shouldBe DerivedService::class
            first.value shouldBe 200

            first shouldBeSameInstanceAs second
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services" {

        val blueprint = kontainer {
            // This one must stay a GlobalSingleton
            singleton(SomeIndependentService::class)

            // this one injects InjectingService and has to become SemiDynamic
            singleton(DeeperInjectingService::class)
            // this one injects InjectingService SimpleService and has to become SemiDynamic
            singleton(InjectingService::class)

            // this one has to be Dynamic
            dynamic(CounterService::class)
            // this one has to be Dynamic
            dynamic(AnotherSimpleService::class)
        }

        val subject = blueprint.create {
            with { CounterService() }
            with { AnotherSimpleService() }
        }

        assertSoftly {

            subject.getProvider<SomeIndependentService>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<DeeperInjectingService>().type shouldBe ServiceProvider.Type.SemiDynamic

            subject.getProvider<InjectingService>().type shouldBe ServiceProvider.Type.SemiDynamic

            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.DynamicOverride
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services by base type" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Base)

        val blueprint = kontainer {

            dynamic(Impl::class)

            singleton(Injecting::class)
        }

        val subject = blueprint.create {
            with { Impl() }
        }

        assertSoftly {

            subject.getProvider<Impl>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services that is defined with base type" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Base)

        val blueprint = kontainer {

            dynamic(Base::class, Impl::class)

            singleton(Injecting::class)
        }

        val subject = blueprint.create {
            with { Impl() }
        }

        assertSoftly {

            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services through builder method" {

        abstract class Base {
            val value = 10
        }

        class Impl : Base()

        data class Injecting(val value: Int)

        val blueprint = kontainer {

            dynamic(Base::class, Impl::class)

            singleton(Injecting::class) { base: Base -> Injecting(base.value) }
        }

        val subject = blueprint.create {
            with { Impl() }
        }

        assertSoftly {

            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
            subject.get<Injecting>().value shouldBe 10
        }
    }

    "Singletons become semi-dynamic when injecting dynamic services lazily" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Lazy<Base>)

        val blueprint = kontainer {

            dynamic(Impl::class)

            singleton(Injecting::class)
        }

        val subject = blueprint.create { with { Impl() } }

        assertSoftly {

            subject.getProvider<Impl>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons do not become semi-dynamic when injecting non dynamic services lazily" {

        abstract class Base
        class Impl : Base()

        data class Injecting(val service: Lazy<Base>)

        val blueprint = kontainer {
            singleton(Impl::class)
            singleton(Injecting::class)
        }

        val subject = blueprint.create()

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

        val blueprint = kontainer {
            singleton(Injecting::class)
            singleton(ImplOne::class)
            dynamic(ImplTwo::class)
        }

        val subject = blueprint.create { with { ImplTwo() } }

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons do not become semi-dynamic when injecting a list, where none in the list is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: List<Base>)

        val blueprint = kontainer {
            singleton(Injecting::class)
            singleton(ImplOne::class)
            singleton(ImplTwo::class)
        }

        val subject = blueprint.create()

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

        val blueprint = kontainer {
            singleton(Injecting::class)
            singleton(ImplOne::class)
            dynamic(ImplTwo::class)
        }

        val subject = blueprint.create { with { ImplTwo() } }

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.DynamicOverride

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.SemiDynamic
        }
    }

    "Singletons do not become semi-dynamic when lazily injecting a list, where none in the list is dynamic" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lazy<List<Base>>)

        val blueprint = kontainer {
            singleton(Injecting::class)
            singleton(ImplOne::class)
            singleton(ImplTwo::class)
        }

        val subject = blueprint.create()

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
            singleton(Injecting::class)
            singleton(ImplOne::class)
            dynamic(ImplTwo::class)
        }

        val subjectOne = blueprint.create { with { ImplTwo() } }
        val first = subjectOne.get<Injecting>()

        val subjectTwo = blueprint.create { with { ImplTwo() } }
        val second = subjectTwo.get<Injecting>()

        assertSoftly {

            subjectOne.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton
            // ImplOne must be the same instance in both containers as it is a GlobalSingleton
            first.all.get(ImplOne::class) shouldBeSameInstanceAs second.all.get(ImplOne::class)

            subjectOne.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.DynamicOverride
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

        val blueprint = kontainer {
            singleton(Injecting::class)
            singleton(ImplOne::class)
            singleton(ImplTwo::class)
        }

        val subject = blueprint.create()

        assertSoftly {

            subject.getProvider<ImplOne>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<ImplTwo>().type shouldBe ServiceProvider.Type.Singleton

            subject.getProvider<Injecting>().type shouldBe ServiceProvider.Type.Singleton
        }
    }
})
