package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
import io.peekandpoke.ultra.kontainer.kontainer

/**
 * Pins how far the promotion of a singleton to SemiDynamic spreads, and where it stops.
 */
class PromotionCharacterizationSpec : StringSpec({

    "Promotion reaches every singleton above a dynamic, also through two parents" {

        class Dyn : Counted()
        class Left(val dyn: Dyn) : Counted()
        class Right(val dyn: Dyn) : Counted()
        class Top(val left: Left, val right: Right) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Top::class)
            singleton(Left::class)
            singleton(Right::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()
        val topOne = one.get(Top::class)
        val topTwo = two.get(Top::class)

        assertSoftly {
            one.typeOf(Top::class) shouldBe Type.SemiDynamic
            one.typeOf(Left::class) shouldBe Type.SemiDynamic
            one.typeOf(Right::class) shouldBe Type.SemiDynamic
            one.typeOf(Dyn::class) shouldBe Type.Dynamic

            Births.of(Top::class) shouldBe 2
            Births.of(Left::class) shouldBe 2
            Births.of(Right::class) shouldBe 2
            Births.of(Dyn::class) shouldBe 2

            withClue("both parents share the one dynamic of their kontainer") {
                topOne.left.dyn shouldBeSameInstanceAs topOne.right.dyn
                topOne.left.dyn shouldBeSameInstanceAs one.get(Dyn::class)
                topTwo.left.dyn shouldNotBeSameInstanceAs topOne.left.dyn
            }
        }
    }

    "Promotion does not spread down: a singleton injected by a SemiDynamic stays a global singleton" {

        class Dyn : Counted()
        class Below : Counted()
        class Promoted(val dyn: Dyn, val below: Below) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Promoted::class)
            singleton(Below::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()
        val promotedOne = one.get(Promoted::class)
        val promotedTwo = two.get(Promoted::class)

        assertSoftly {
            one.typeOf(Promoted::class) shouldBe Type.SemiDynamic
            one.typeOf(Below::class) shouldBe Type.Singleton

            Births.of(Promoted::class) shouldBe 2
            Births.of(Below::class) shouldBe 1

            promotedOne shouldNotBeSameInstanceAs promotedTwo
            promotedOne.below shouldBeSameInstanceAs promotedTwo.below
        }
    }

    "Promotion does not spread sideways: an unrelated singleton stays global and is shared by two kontainers" {

        class Dyn : Counted()
        class Promoted(val dyn: Dyn) : Counted()
        class Unrelated : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Promoted::class)
            singleton(Unrelated::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(Unrelated::class) shouldBe Type.Singleton
            two.typeOf(Unrelated::class) shouldBe Type.Singleton

            one.get(Unrelated::class) shouldBeSameInstanceAs two.get(Unrelated::class)
            Births.of(Unrelated::class) shouldBe 1
        }
    }

    "Promotion does not reach singletons off the chain: a singleton injecting a plain singleton stays global" {

        class Dyn : Counted()
        class Promoted(val dyn: Dyn) : Counted()
        class Plain : Counted()
        class InjectsPlain(val plain: Plain) : Counted()

        val blueprint = kontainer {
            singleton(Promoted::class)
            singleton(Plain::class)
            singleton(InjectsPlain::class)
            dynamic(Dyn::class)
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.typeOf(Promoted::class) shouldBe Type.SemiDynamic
            subject.typeOf(Plain::class) shouldBe Type.Singleton
            subject.typeOf(InjectsPlain::class) shouldBe Type.Singleton
        }
    }

    "Promotion passes through prototypes: singleton -> prototype -> prototype -> dynamic" {

        class Dyn : Counted()
        class ProtoLow(val dyn: Dyn) : Counted()
        class ProtoHigh(val low: ProtoLow) : Counted()
        class Top(val high: ProtoHigh) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(Top::class)
            prototype(ProtoHigh::class)
            prototype(ProtoLow::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()
        val topOne = one.get(Top::class)
        val topTwo = two.get(Top::class)

        assertSoftly {
            one.typeOf(Top::class) shouldBe Type.SemiDynamic
            one.typeOf(ProtoHigh::class) shouldBe Type.Prototype
            one.typeOf(ProtoLow::class) shouldBe Type.Prototype

            Births.of(Top::class) shouldBe 2
            Births.of(ProtoHigh::class) shouldBe 2
            Births.of(ProtoLow::class) shouldBe 2
            Births.of(Dyn::class) shouldBe 2

            topOne.high.low.dyn shouldBeSameInstanceAs one.get(Dyn::class)
            topTwo.high.low.dyn shouldBeSameInstanceAs two.get(Dyn::class)
        }
    }

    "Promotion through an interface: the dynamic is registered by its class and injected by its interface" {

        abstract class Port
        class DynAdapter : Port()
        class User(val port: Port) : Counted()

        val blueprint = kontainer {
            singleton(User::class)
            dynamic(DynAdapter::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(User::class) shouldBe Type.SemiDynamic
            one.typeOf(DynAdapter::class) shouldBe Type.Dynamic

            one.get(User::class).port shouldBeSameInstanceAs one.get(DynAdapter::class)
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)
        }
    }

    "Promotion through an interface, transitively: a singleton implementing the interface injects a dynamic" {

        class Dyn : Counted()
        abstract class Port
        class Adapter(val dyn: Dyn) : Port()
        class User(val port: Port) : Counted()

        val blueprint = kontainer {
            singleton(User::class)
            singleton(Adapter::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(User::class) shouldBe Type.SemiDynamic
            one.typeOf(Adapter::class) shouldBe Type.SemiDynamic

            (one.get(User::class).port as Adapter).dyn shouldBeSameInstanceAs one.get(Dyn::class)
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)
        }
    }

    "Promotion through a service registered under its super type: dynamic(Base, Impl) injected as Base" {

        abstract class Base
        class Impl : Base()
        class User(val base: Base) : Counted()

        val subject = kontainer {
            singleton(User::class)
            dynamic(Base::class, Impl::class)
        }.create()

        assertSoftly {
            subject.typeOf(User::class) shouldBe Type.SemiDynamic
            subject.typeOf(Base::class) shouldBe Type.Dynamic
        }
    }

    "A prototype above a SemiDynamic stays Prototype and sees the SemiDynamic of its kontainer" {

        class Dyn : Counted()
        class Promoted(val dyn: Dyn) : Counted()
        class Proto(val promoted: Promoted) : Counted()

        val blueprint = kontainer {
            prototype(Proto::class)
            singleton(Promoted::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(Proto::class) shouldBe Type.Prototype
            one.typeOf(Promoted::class) shouldBe Type.SemiDynamic

            one.get(Proto::class).promoted shouldBeSameInstanceAs one.get(Promoted::class)
            two.get(Proto::class).promoted shouldBeSameInstanceAs two.get(Promoted::class)
            one.get(Promoted::class) shouldNotBeSameInstanceAs two.get(Promoted::class)
        }
    }

    "A dynamic above a SemiDynamic stays Dynamic" {

        class Dyn : Counted()
        class Promoted(val dyn: Dyn) : Counted()
        class DynTop(val promoted: Promoted) : Counted()

        val subject = kontainer {
            dynamic(DynTop::class)
            singleton(Promoted::class)
            dynamic(Dyn::class)
        }.create()

        assertSoftly {
            subject.typeOf(DynTop::class) shouldBe Type.Dynamic
            subject.typeOf(Promoted::class) shouldBe Type.SemiDynamic
        }
    }

    "Injecting the Kontainer promotes a singleton: Kontainer is registered as a dynamic" {

        class UsesKontainer(val kontainer: Kontainer) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(UsesKontainer::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(Kontainer::class) shouldBe Type.Dynamic
            one.typeOf(UsesKontainer::class) shouldBe Type.SemiDynamic

            one.get(UsesKontainer::class).kontainer shouldBeSameInstanceAs one
            two.get(UsesKontainer::class).kontainer shouldBeSameInstanceAs two
            Births.of(UsesKontainer::class) shouldBe 2
        }
    }

    "Injecting the KontainerBlueprint does not promote: it is registered as a singleton" {

        class UsesBlueprint(val blueprint: KontainerBlueprint) : Counted()

        Births.reset()

        val blueprint = kontainer {
            singleton(UsesBlueprint::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.typeOf(KontainerBlueprint::class) shouldBe Type.Singleton
            one.typeOf(UsesBlueprint::class) shouldBe Type.Singleton

            one.get(UsesBlueprint::class) shouldBeSameInstanceAs two.get(UsesBlueprint::class)
            Births.of(UsesBlueprint::class) shouldBe 1
        }
    }

    "Promotion is decided per blueprint: the same singleton class is global in one blueprint and SemiDynamic in another" {

        class Dep : Counted()
        class User(val dep: Dep) : Counted()

        val withSingleton = kontainer {
            singleton(User::class)
            singleton(Dep::class)
        }

        val withDynamic = kontainer {
            singleton(User::class)
            dynamic(Dep::class)
        }

        assertSoftly {
            withSingleton.create().typeOf(User::class) shouldBe Type.Singleton
            withDynamic.create().typeOf(User::class) shouldBe Type.SemiDynamic
        }
    }
})
