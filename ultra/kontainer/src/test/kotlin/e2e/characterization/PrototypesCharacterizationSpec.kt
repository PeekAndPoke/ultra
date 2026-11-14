package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
import io.peekandpoke.ultra.kontainer.kontainer

/**
 * Pins prototypes: a new instance per injection point and per get, wherever they are injected.
 */
class PrototypesCharacterizationSpec : StringSpec({

    "A prototype is new on every get, and the kontainer counts each one" {

        class Proto : Counted()

        Births.reset()

        val subject = kontainer {
            prototype(Proto::class)
        }.create()

        assertSoftly {
            subject.get(Proto::class) shouldNotBeSameInstanceAs subject.get(Proto::class)
            Births.of(Proto::class) shouldBe 2
            subject.getProvider(Proto::class).instances.size shouldBe 2
            subject.typeOf(Proto::class) shouldBe Type.Prototype
        }
    }

    "A prototype is new for every injection point of one service" {

        class Proto : Counted()
        class Twice(val first: Proto, val second: Proto)

        Births.reset()

        val subject = kontainer {
            prototype(Proto::class)
            singleton(Twice::class)
        }.create()

        val twice = subject.get(Twice::class)

        assertSoftly {
            twice.first shouldNotBeSameInstanceAs twice.second
            Births.of(Proto::class) shouldBe 2
        }
    }

    "A prototype injected into a singleton is built once, for the one singleton, shared by all kontainers" {

        class Proto : Counted()
        class Single(val proto: Proto) : Counted()

        Births.reset()

        val blueprint = kontainer {
            prototype(Proto::class)
            singleton(Single::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Single::class).proto shouldBeSameInstanceAs two.get(Single::class).proto
            Births.of(Proto::class) shouldBe 1
            one.typeOf(Single::class) shouldBe Type.Singleton
        }
    }

    "A prototype injected into a dynamic is built once per kontainer" {

        class Proto : Counted()
        class Dyn(val proto: Proto) : Counted()

        Births.reset()

        val blueprint = kontainer {
            prototype(Proto::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Dyn::class).proto shouldBeSameInstanceAs one.get(Dyn::class).proto
            one.get(Dyn::class).proto shouldNotBeSameInstanceAs two.get(Dyn::class).proto
            Births.of(Proto::class) shouldBe 2
        }
    }

    "A prototype injected into another prototype is new every time" {

        class Inner : Counted()
        class Outer(val inner: Inner) : Counted()

        Births.reset()

        val subject = kontainer {
            prototype(Inner::class)
            prototype(Outer::class)
        }.create()

        assertSoftly {
            subject.get(Outer::class).inner shouldNotBeSameInstanceAs subject.get(Outer::class).inner
            Births.of(Outer::class) shouldBe 2
            Births.of(Inner::class) shouldBe 2
        }
    }

    "A prototype sees the dynamic of the kontainer it is built in" {

        class Dyn : Counted()
        class Proto(val dyn: Dyn) : Counted()

        Births.reset()

        val blueprint = kontainer {
            prototype(Proto::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Proto::class).dyn shouldBeSameInstanceAs one.get(Dyn::class)
            one.get(Proto::class).dyn shouldBeSameInstanceAs one.get(Proto::class).dyn
            two.get(Proto::class).dyn shouldBeSameInstanceAs two.get(Dyn::class)
            one.get(Dyn::class) shouldNotBeSameInstanceAs two.get(Dyn::class)

            Births.of(Dyn::class) shouldBe 2
            one.typeOf(Proto::class) shouldBe Type.Prototype
        }
    }

    "A prototype sees the override of the kontainer it is built in" {

        class Dyn(val value: Int)
        class Proto(val dyn: Dyn)

        val blueprint = kontainer {
            prototype(Proto::class)
            dynamic(Dyn::class) { Dyn(0) }
        }

        val one = blueprint.create { with { Dyn(1) } }
        val two = blueprint.create { with { Dyn(2) } }

        assertSoftly {
            one.get(Proto::class).dyn.value shouldBe 1
            two.get(Proto::class).dyn.value shouldBe 2
        }
    }

    "A prototype sees the global singleton shared by all kontainers" {

        class Single : Counted()
        class Proto(val single: Single)

        Births.reset()

        val blueprint = kontainer {
            prototype(Proto::class)
            singleton(Single::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.get(Proto::class).single shouldBeSameInstanceAs two.get(Proto::class).single
            Births.of(Single::class) shouldBe 1
        }
    }

    "Each kontainer has its own prototype provider: the recorded instances are per kontainer" {

        class Proto

        val blueprint = kontainer {
            prototype(Proto::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        repeat(3) { one.get(Proto::class) }
        two.get(Proto::class)

        assertSoftly {
            one.getProvider(Proto::class) shouldNotBeSameInstanceAs two.getProvider(Proto::class)
            one.getProvider(Proto::class).instances.size shouldBe 3
            two.getProvider(Proto::class).instances.size shouldBe 1
        }
    }

    "A global singleton's provider is shared: instances it records show up in every kontainer" {

        class Single

        val blueprint = kontainer {
            singleton(Single::class)
        }

        val one = blueprint.create()
        one.get(Single::class)
        val two = blueprint.create()

        withClue("kontainer two reports the instance that kontainer one created") {
            two.getProvider(Single::class).instances.size shouldBe 1
            two.getProvider(Single::class) shouldBeSameInstanceAs one.getProvider(Single::class)
        }
    }

    "A SemiDynamic's provider is per kontainer" {

        class Dyn
        class Promoted(val dyn: Dyn)

        val blueprint = kontainer {
            singleton(Promoted::class)
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        one.get(Promoted::class)
        val two = blueprint.create()

        assertSoftly {
            two.getProvider(Promoted::class) shouldNotBeSameInstanceAs one.getProvider(Promoted::class)
            two.getProvider(Promoted::class).instances.size shouldBe 0
        }
    }
})
