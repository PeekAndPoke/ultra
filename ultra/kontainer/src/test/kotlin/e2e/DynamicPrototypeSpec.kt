package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceNotFound
import io.peekandpoke.ultra.kontainer.e2e.characterization.Births
import io.peekandpoke.ultra.kontainer.e2e.characterization.Counted
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins `dynamicPrototype()`: created like a prototype, bound to the kontainer like a dynamic.
 */
class DynamicPrototypeSpec : StringSpec({

    "Declared by class: Prototype.Dynamic, new on every get and for every injection point" {

        class DynProto : Counted()
        class Twice(val first: DynProto, val second: DynProto)

        Births.reset()

        val subject = kontainer {
            dynamicPrototype(DynProto::class)
            prototype(Twice::class)
        }.create()

        assertSoftly {
            subject.reportedOf(DynProto::class) shouldBe Reported.DynamicPrototype

            subject.get(DynProto::class) shouldNotBeSameInstanceAs subject.get(DynProto::class)
            Births.of(DynProto::class) shouldBe 2

            val twice = subject.get(Twice::class)
            twice.first shouldNotBeSameInstanceAs twice.second
            Births.of(DynProto::class) shouldBe 4
        }
    }

    "Declared with a base and an implementation class" {

        abstract class Base
        class Impl : Base()

        val subject = kontainer {
            dynamicPrototype(Base::class, Impl::class)
        }.create()

        assertSoftly {
            subject.get(Base::class)::class shouldBe Impl::class
            subject.reportedOf(Base::class) shouldBe Reported.DynamicPrototype

            shouldThrow<ServiceNotFound> { subject.get(Impl::class) }
        }
    }

    "Declared by a factory without parameters: the factory runs on every get, never before" {

        class DynProto

        val runs = AtomicInteger()

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class) { runs.incrementAndGet(); DynProto() }
        }

        val subject = blueprint.create()

        assertSoftly {
            runs.get() shouldBe 0
            subject.get(DynProto::class) shouldNotBeSameInstanceAs subject.get(DynProto::class)
            runs.get() shouldBe 2
            subject.reportedOf(DynProto::class) shouldBe Reported.DynamicPrototype
        }
    }

    "Declared by a factory with parameters: the factory runs on every get with the kontainer's services" {

        class Single
        class Dyn
        class DynProto(val single: Single, val dyn: Dyn)

        val runs = AtomicInteger()

        val blueprint = kontainer {
            singleton(Single::class)
            dynamic(Dyn::class)
            dynamicPrototype(DynProto::class) { single: Single, dyn: Dyn ->
                runs.incrementAndGet()
                DynProto(single, dyn)
            }
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(DynProto::class).dyn shouldBeSameInstanceAs one.get(Dyn::class)
            two.get(DynProto::class).dyn shouldBeSameInstanceAs two.get(Dyn::class)
            one.get(DynProto::class).single shouldBeSameInstanceAs two.get(DynProto::class).single
            runs.get() shouldBe 4
            one.reportedOf(DynProto::class) shouldBe Reported.DynamicPrototype
        }
    }

    "A singleton injecting a dynamicPrototype that injects nothing becomes SemiDynamic, one per kontainer" {

        class DynProto : Counted()
        class User(val dynProto: DynProto) : Counted()

        Births.reset()

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
            singleton(User::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton

            one.get(User::class) shouldBeSameInstanceAs one.get(User::class)
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)

            Births.of(User::class) shouldBe 2
            Births.of(DynProto::class) shouldBe 2
        }
    }

    "A plain prototype that injects nothing does not promote: the contrast to dynamicPrototype" {

        class Proto
        class User(val proto: Proto)

        val blueprint = kontainer {
            prototype(Proto::class)
            singleton(User::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Proto::class) shouldBe Reported.StaticPrototype
            one.reportedOf(User::class) shouldBe Reported.StaticSingleton
            one.get(User::class) shouldBeSameInstanceAs two.get(User::class)
        }
    }

    "Promotion through Lazy, List and Lookup of a dynamicPrototype" {

        abstract class Base
        class DynProto : Base()
        class ViaLazy(val lazy: Lazy<DynProto>)
        class ViaList(val all: List<Base>)
        class ViaLookup(val lookup: Lookup<Base>)

        val subject = kontainer {
            dynamicPrototype(DynProto::class)
            singleton(ViaLazy::class)
            singleton(ViaList::class)
            singleton(ViaLookup::class)
        }.create()

        assertSoftly {
            subject.reportedOf(ViaLazy::class) shouldBe Reported.SemiDynamicSingleton
            subject.reportedOf(ViaList::class) shouldBe Reported.SemiDynamicSingleton
            subject.reportedOf(ViaLookup::class) shouldBe Reported.SemiDynamicSingleton

            val lookup = subject.get(ViaLookup::class).lookup
            lookup.get(DynProto::class) shouldNotBeSameInstanceAs lookup.get(DynProto::class)
        }
    }

    "With the InjectionContext: each instance gets the context of its injection, as Log needs" {

        class Named(val context: InjectionContext)
        class First(val named: Named)
        class Second(val named: Named)

        val subject = kontainer {
            dynamicPrototype(Named::class)
            singleton(First::class)
            singleton(Second::class)
        }.create()

        assertSoftly {
            subject.get(Named::class).context.requestingClass shouldBe Kontainer::class
            subject.get(First::class).named.context.requestingClass shouldBe First::class
            subject.get(Second::class).named.context.requestingClass shouldBe Second::class

            subject.reportedOf(First::class) shouldBe Reported.SemiDynamicSingleton
        }
    }

    "Each kontainer has its own provider: the recorded instances are per kontainer" {

        class DynProto

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        repeat(3) { one.get(DynProto::class) }
        two.get(DynProto::class)

        assertSoftly {
            one.getProvider(DynProto::class) shouldNotBeSameInstanceAs two.getProvider(DynProto::class)
            one.getProvider(DynProto::class).instances.size shouldBe 3
            two.getProvider(DynProto::class).instances.size shouldBe 1
        }
    }

    "An override at create is refused with KontainerInconsistent, saying only dynamic singletons can be overridden" {

        class DynProto

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { DynProto() } }
        }

        error.message shouldBe "Unexpected dynamics were provided: ${DynProto::class.getName()}" +
                " (only dynamic singletons can be overridden, not the dynamic prototype " +
                "${DynProto::class.getName()})"
    }

    "Overrides for a singleton next to a dynamicPrototype: the message names only what concerns them" {

        class DynProto
        class Single

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
            singleton(Single::class)
        }

        assertSoftly {
            shouldThrow<KontainerInconsistent> {
                blueprint.create { with { Single() } }
            }.message shouldBe "Unexpected dynamics were provided: ${Single::class.getName()}"
        }
    }

    "An override of the implementation of a dynamicPrototype declared by its base: the hint names the base" {

        abstract class Base
        class Impl : Base()

        val blueprint = kontainer {
            dynamicPrototype(Base::class, Impl::class)
        }

        shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Impl() } }
        }.message shouldBe "Unexpected dynamics were provided: ${Impl::class.getName()}" +
                " (only dynamic singletons can be overridden, not the dynamic prototype ${Base::class.getName()})"
    }

    "An override of a super type of a dynamicPrototype: exactly the old message, no hint" {

        open class Wide
        class DynProto : Wide()

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
        }

        shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Wide() } }
        }.message shouldBe "Unexpected dynamics were provided: ${Wide::class.getName()}"
    }

    "Two refused overrides, one a dynamicPrototype and one a singleton: both named, the hint for the first only" {

        class DynProto
        class Single

        val blueprint = kontainer {
            dynamicPrototype(DynProto::class)
            singleton(Single::class)
        }

        shouldThrow<KontainerInconsistent> {
            blueprint.create {
                with { DynProto() }
                with { Single() }
            }
        }.message shouldBe "Unexpected dynamics were provided: ${DynProto::class.getName()}, ${Single::class.getName()}" +
                " (only dynamic singletons can be overridden, not the dynamic prototype ${DynProto::class.getName()})"
    }

    "An override at clone throws ServiceNotFound, as for any non-dynamic (ULTRA-00332)" {

        class DynProto

        val original = kontainer {
            dynamicPrototype(DynProto::class)
        }.create()

        shouldThrow<ServiceNotFound> {
            original.clone { with { DynProto() } }
        }.message shouldContain "was not found"
    }

    // Redefinitions involving dynamicPrototype. The rule: a later definition keeps the type and kind of the one it
    // replaces, unless that one is a static singleton. Each case is a choice handed to ULTRA-00331.

    "Redefinition: singleton then dynamicPrototype ends as Prototype.Dynamic and promotes its users" {

        class Target
        class User(val target: Target)

        val subject = kontainer {
            singleton(Target::class)
            singleton(User::class)
            dynamicPrototype(Target::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
            subject.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
        }
    }

    // Pinned as it is; it fails open without a signal. Whether Dynamic should win here is ticket ULTRA-00331.
    "CURRENT BEHAVIOUR: prototype then dynamicPrototype stays Prototype.Static, its users stay global, nothing throws" {

        class Target
        class User(val target: Target)

        val blueprint = kontainer {
            prototype(Target::class)
            singleton(User::class)
            dynamicPrototype(Target::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Target::class) shouldBe Reported.StaticPrototype
            one.reportedOf(User::class) shouldBe Reported.StaticSingleton
            one.get(User::class) shouldBeSameInstanceAs two.get(User::class)
        }
    }

    "Redefinition: dynamic then dynamicPrototype stays Singleton.Dynamic and uses the later producer" {

        abstract class Target
        class First : Target()
        class Second : Target()

        val subject = kontainer {
            dynamic(Target::class, First::class)
            dynamicPrototype(Target::class, Second::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            subject.get(Target::class)::class shouldBe Second::class
            subject.get(Target::class) shouldBeSameInstanceAs subject.get(Target::class)
        }
    }

    "Redefinition: dynamicPrototype then singleton stays Prototype.Dynamic and uses the later producer" {

        abstract class Target
        class First : Target()
        class Second : Target()

        val subject = kontainer {
            dynamicPrototype(Target::class, First::class)
            singleton(Target::class, Second::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
            subject.get(Target::class)::class shouldBe Second::class
            subject.get(Target::class) shouldNotBeSameInstanceAs subject.get(Target::class)
        }
    }

    "Redefinition: dynamicPrototype then prototype stays Prototype.Dynamic" {

        class Target

        val subject = kontainer {
            dynamicPrototype(Target::class)
            prototype(Target::class)
        }.create()

        subject.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
    }

    "Redefinition: dynamicPrototype then dynamic stays Prototype.Dynamic and cannot be overridden at create" {

        class Target

        val blueprint = kontainer {
            dynamicPrototype(Target::class)
            dynamic(Target::class)
        }

        assertSoftly {
            blueprint.create().reportedOf(Target::class) shouldBe Reported.DynamicPrototype

            shouldThrow<KontainerInconsistent> {
                blueprint.create { with { Target() } }
            }
        }
    }

    "Redefinition across modules: the later module's dynamicPrototype over a singleton wins" {

        class Target

        val base = module { singleton(Target::class) }
        val overriding = module { dynamicPrototype(Target::class) }

        val subject = kontainer {
            module(base)
            module(overriding)
        }.create()

        subject.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
    }

    "Redefinition across extend skips the rule (ULTRA-00330): a dynamicPrototype redefined as singleton is Singleton" {

        class Target
        class User(val target: Target)

        val base = kontainer {
            dynamicPrototype(Target::class)
            singleton(User::class)
        }

        val extended = base.extend {
            singleton(Target::class)
        }

        assertSoftly {
            base.create().reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton

            withClue("the extension ends the promotion") {
                extended.create().reportedOf(Target::class) shouldBe Reported.StaticSingleton
                extended.create().reportedOf(User::class) shouldBe Reported.StaticSingleton
            }
        }
    }
})
