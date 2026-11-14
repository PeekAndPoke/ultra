package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins, per injection form: whether it counts as a dependency for the promotion to SemiDynamic, when the injected
 * service is built, and how often.
 */
class InjectionFormsCharacterizationSpec : StringSpec({

    "Plain parameter: promotes, the dynamic is built together with the singleton, once per kontainer" {

        class Dyn : Counted()
        class Holder(val dyn: Dyn) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()

        assertSoftly {
            Births.of(Dyn::class) shouldBe 0

            one.get(Holder::class)
            Births.of(Dyn::class) shouldBe 1

            one.get(Holder::class)
            blueprint.create().get(Holder::class)
            Births.of(Holder::class) shouldBe 2
            Births.of(Dyn::class) shouldBe 2

            one.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "Nullable parameter of a registered dynamic: promotes, built with the singleton, once per kontainer" {

        class Dyn : Counted()
        class Holder(val dyn: Dyn?) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            val holderOne = one.get(Holder::class)
            Births.of(Dyn::class) shouldBe 1
            holderOne.dyn shouldBeSameInstanceAs one.get(Dyn::class)

            two.get(Holder::class).dyn shouldBeSameInstanceAs two.get(Dyn::class)
            Births.of(Dyn::class) shouldBe 2
            Births.of(Holder::class) shouldBe 2

            one.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "Nullable parameter of a service that is not registered: no dependency, injects null, stays global" {

        class Missing
        class Holder(val missing: Missing?) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(Holder::class) shouldBe Type.Singleton
            one.get(Holder::class).missing.shouldBeNull()
            one.get(Holder::class) shouldBeSameInstanceAs two.get(Holder::class)
            Births.of(Holder::class) shouldBe 1
        }
    }

    "Lazy<T> of a dynamic: promotes, the dynamic is built on first access of the value, once per kontainer" {

        class Dyn : Counted()
        class Holder(val dyn: Lazy<Dyn>) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            val holderOne = one.get(Holder::class)

            withClue("not built when the singleton is built") {
                Births.of(Dyn::class) shouldBe 0
            }

            holderOne.dyn.value shouldBeSameInstanceAs one.get(Dyn::class)
            holderOne.dyn.value shouldBeSameInstanceAs one.get(Dyn::class)
            Births.of(Dyn::class) shouldBe 1

            two.get(Holder::class).dyn.value shouldBeSameInstanceAs two.get(Dyn::class)
            Births.of(Dyn::class) shouldBe 2
            Births.of(Holder::class) shouldBe 2

            one.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "Lazy<T?> of a registered dynamic: promotes" {

        class Dyn : Counted()
        class Holder(val dyn: Lazy<Dyn?>) : Counted()

        val subject = kontainer {
            singleton(Holder::class)
            dynamic(Dyn::class)
        }.create()

        assertSoftly {
            subject.typeOf(Holder::class) shouldBe Type.SemiDynamic
            subject.get(Holder::class).dyn.value shouldBeSameInstanceAs subject.get(Dyn::class)
        }
    }

    "Lazy<T> of a prototype in a singleton: the prototype is built on first access, then the Lazy keeps it" {

        class Proto : Counted()
        class Holder(val proto: Lazy<Proto>) : Counted()

        Births.reset()

        val subject = kontainer {
            singleton(Holder::class)
            prototype(Proto::class)
        }.create()

        val holder = subject.get(Holder::class)

        assertSoftly {
            subject.typeOf(Holder::class) shouldBe Type.Singleton
            Births.of(Proto::class) shouldBe 0

            holder.proto.value shouldBeSameInstanceAs holder.proto.value
            Births.of(Proto::class) shouldBe 1
        }
    }

    "List<T> with a dynamic among the candidates: promotes, all candidates are built with the singleton" {

        abstract class Base : Counted()
        class SingleImpl : Base()
        class DynImpl : Base()
        class ProtoImpl : Base()
        class Holder(val all: List<Base>) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
            singleton(SingleImpl::class)
            dynamic(DynImpl::class)
            prototype(ProtoImpl::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            val holderOne = one.get(Holder::class)

            Births.of(SingleImpl::class) shouldBe 1
            Births.of(DynImpl::class) shouldBe 1
            Births.of(ProtoImpl::class) shouldBe 1

            holderOne.all.map { it::class }.toSet() shouldBe setOf(SingleImpl::class, DynImpl::class, ProtoImpl::class)
            holderOne.all.first { it is DynImpl } shouldBeSameInstanceAs one.get(DynImpl::class)

            val holderTwo = two.get(Holder::class)
            holderTwo shouldNotBeSameInstanceAs holderOne
            holderTwo.all.first { it is SingleImpl } shouldBeSameInstanceAs holderOne.all.first { it is SingleImpl }

            Births.of(Holder::class) shouldBe 2
            Births.of(SingleImpl::class) shouldBe 1
            Births.of(DynImpl::class) shouldBe 2
            Births.of(ProtoImpl::class) shouldBe 2

            one.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "Lazy<List<T>> with a dynamic among the candidates: promotes, the candidates are built on first access" {

        abstract class Base : Counted()
        class SingleImpl : Base()
        class DynImpl : Base()
        class Holder(val all: Lazy<List<Base>>) : Counted()

        Births.reset()

        val subject = kontainer {
            singleton(Holder::class)
            singleton(SingleImpl::class)
            dynamic(DynImpl::class)
        }.create()

        assertSoftly {
            val holder = subject.get(Holder::class)

            Births.of(SingleImpl::class) shouldBe 0
            Births.of(DynImpl::class) shouldBe 0

            holder.all.value.size shouldBe 2
            holder.all.value.size shouldBe 2
            Births.of(SingleImpl::class) shouldBe 1
            Births.of(DynImpl::class) shouldBe 1

            subject.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "Lookup<T> with a dynamic among the candidates: promotes, builds on get, a prototype is new on every get" {

        abstract class Base : Counted()
        class DynImpl : Base()
        class ProtoImpl : Base()
        class Holder(val lookup: Lookup<Base>) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Holder::class)
            dynamic(DynImpl::class)
            prototype(ProtoImpl::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            val holderOne = one.get(Holder::class)

            Births.of(DynImpl::class) shouldBe 0
            Births.of(ProtoImpl::class) shouldBe 0

            holderOne.lookup.has(DynImpl::class) shouldBe true
            holderOne.lookup.has(ProtoImpl::class) shouldBe true
            Births.of(DynImpl::class) shouldBe 0

            holderOne.lookup.get(DynImpl::class) shouldBeSameInstanceAs one.get(DynImpl::class)
            holderOne.lookup.get(DynImpl::class) shouldBeSameInstanceAs one.get(DynImpl::class)
            Births.of(DynImpl::class) shouldBe 1

            holderOne.lookup.get(ProtoImpl::class) shouldNotBeSameInstanceAs holderOne.lookup.get(ProtoImpl::class)
            Births.of(ProtoImpl::class) shouldBe 2

            two.get(Holder::class).lookup.get(DynImpl::class) shouldBeSameInstanceAs two.get(DynImpl::class)
            Births.of(DynImpl::class) shouldBe 2
            Births.of(Holder::class) shouldBe 2

            one.typeOf(Holder::class) shouldBe Type.SemiDynamic
        }
    }

    "CURRENT BEHAVIOUR: a global singleton's Lookup stays bound to the first kontainer, which records every prototype" {

        abstract class Base : Counted()
        class ProtoImpl : Base()
        class Holder(val lookup: Lookup<Base>) : Counted()

        val blueprint = kontainer {
            singleton(Holder::class)
            prototype(ProtoImpl::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        one.get(Holder::class)

        assertSoftly {
            one.typeOf(Holder::class) shouldBe Type.Singleton
            two.get(Holder::class) shouldBeSameInstanceAs one.get(Holder::class)

            repeat(3) { two.get(Holder::class).lookup.get(ProtoImpl::class) }

            withClue("the prototypes are created by kontainer one's provider, although used through kontainer two") {
                one.getProvider(ProtoImpl::class).instances.size shouldBe 3
                two.getProvider(ProtoImpl::class).instances.size shouldBe 0
            }
        }
    }

    "InjectionContext in a singleton: promotes, built once per kontainer, keeps the context of its first requester" {

        class WithContext(val context: InjectionContext) : Counted()
        class FirstUser(val ctx: WithContext)
        class SecondUser(val ctx: WithContext)

        Births.reset()

        val blueprint = kontainer {
            singleton(WithContext::class)
            prototype(FirstUser::class)
            prototype(SecondUser::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(InjectionContext::class) shouldBe Type.Dynamic
            one.typeOf(WithContext::class) shouldBe Type.SemiDynamic

            one.get(FirstUser::class).ctx.context.requestingClass shouldBe FirstUser::class
            one.get(SecondUser::class).ctx.context.requestingClass shouldBe FirstUser::class

            two.get(SecondUser::class).ctx.context.requestingClass shouldBe SecondUser::class

            Births.of(WithContext::class) shouldBe 2
        }
    }

    "InjectionContext in a prototype: stays Prototype, each instance gets the context of its injection" {

        class WithContext(val context: InjectionContext) : Counted()
        class User(val ctx: WithContext)

        val subject = kontainer {
            prototype(WithContext::class)
            prototype(User::class)
        }.create()

        assertSoftly {
            subject.typeOf(WithContext::class) shouldBe Type.Prototype

            subject.get(WithContext::class).context.requestingClass shouldBe Kontainer::class
            subject.get(User::class).ctx.context.requestingClass shouldBe User::class
        }
    }

    "InjectionContext in a dynamic: stays Dynamic" {

        class WithContext(val context: InjectionContext) : Counted()

        val subject = kontainer {
            dynamic(WithContext::class)
        }.create()

        assertSoftly {
            subject.typeOf(WithContext::class) shouldBe Type.Dynamic
        }
    }

    "Kontainer in a prototype: stays Prototype and sees the kontainer it was built in" {

        class UsesKontainer(val kontainer: Kontainer) : Counted()

        val blueprint = kontainer {
            prototype(UsesKontainer::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(UsesKontainer::class) shouldBe Type.Prototype
            one.get(UsesKontainer::class).kontainer shouldBeSameInstanceAs one
            two.get(UsesKontainer::class).kontainer shouldBeSameInstanceAs two
        }
    }

    "Factory with several parameters and nothing dynamic: stays global, the factory runs once for all kontainers" {

        class Single : Counted()
        class Proto : Counted()
        class Built(val single: Single, val proto: Proto, val again: Proto)

        val runs = AtomicInteger()
        Births.reset()

        val blueprint = kontainer {
            singleton(Single::class)
            prototype(Proto::class)
            singleton(Built::class) { single: Single, proto: Proto, again: Proto ->
                runs.incrementAndGet()
                Built(single, proto, again)
            }
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Built::class) shouldBeSameInstanceAs two.get(Built::class)
            one.typeOf(Built::class) shouldBe Type.Singleton

            runs.get() shouldBe 1
            Births.of(Single::class) shouldBe 1

            withClue("two prototype parameters get two instances") {
                Births.of(Proto::class) shouldBe 2
                one.get(Built::class).proto shouldNotBeSameInstanceAs one.get(Built::class).again
            }
        }
    }

    "Factory with several parameters, one of them dynamic: promotes, the factory runs once per kontainer" {

        class Single : Counted()
        class Proto : Counted()
        class Dyn : Counted()
        class Built(val single: Single, val proto: Proto, val dyn: Dyn)

        val runs = AtomicInteger()
        Births.reset()

        val blueprint = kontainer {
            singleton(Single::class)
            prototype(Proto::class)
            dynamic(Dyn::class)
            singleton(Built::class) { single: Single, proto: Proto, dyn: Dyn ->
                runs.incrementAndGet()
                Built(single, proto, dyn)
            }
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Built::class) shouldNotBeSameInstanceAs two.get(Built::class)
            one.get(Built::class).dyn shouldBeSameInstanceAs one.get(Dyn::class)
            one.get(Built::class).single shouldBeSameInstanceAs two.get(Built::class).single
            one.typeOf(Built::class) shouldBe Type.SemiDynamic

            runs.get() shouldBe 2
            Births.of(Single::class) shouldBe 1
            Births.of(Proto::class) shouldBe 2
            Births.of(Dyn::class) shouldBe 2
        }
    }

    "Factory with a Lazy parameter of a dynamic: promotes" {

        class Dyn : Counted()
        class Built(val dyn: Lazy<Dyn>)

        val subject = kontainer {
            dynamic(Dyn::class)
            singleton(Built::class) { dyn: Lazy<Dyn> -> Built(dyn) }
        }.create()

        assertSoftly {
            subject.typeOf(Built::class) shouldBe Type.SemiDynamic
        }
    }

    "Factory without parameters declared as dynamic: runs once per kontainer, lazily" {

        class Dyn : Counted()

        val runs = AtomicInteger()

        val blueprint = kontainer {
            dynamic(Dyn::class) { runs.incrementAndGet(); Dyn() }
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            runs.get() shouldBe 0

            one.get(Dyn::class)
            one.get(Dyn::class)
            two.get(Dyn::class)

            runs.get() shouldBe 2
            one.typeOf(Dyn::class) shouldBe Type.Dynamic
        }
    }
})
