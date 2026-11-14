package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceAmbiguous
import io.peekandpoke.ultra.kontainer.ServiceNotFound
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins what `create { with { } }` does: overrides of dynamics, dynamics without one, and overrides that are refused.
 */
class DynamicsAtCreateCharacterizationSpec : StringSpec({

    "An override replaces the dynamic: reported as DynamicOverride, the default producer never runs" {

        open class Dyn(val value: Int) : Counted()
        class Holder(val dyn: Dyn) : Counted()

        val defaults = AtomicInteger()
        Births.reset()

        val blueprint = kontainer {
            dynamic(Dyn::class) { defaults.incrementAndGet(); Dyn(1) }
            singleton(Holder::class)
        }

        val given = Dyn(2)
        val subject = blueprint.create { with { given } }

        assertSoftly {
            subject.typeOf(Dyn::class) shouldBe Type.DynamicOverride
            subject.typeOf(Holder::class) shouldBe Type.SemiDynamic

            subject.get(Dyn::class) shouldBeSameInstanceAs given
            subject.get(Holder::class).dyn shouldBeSameInstanceAs given

            defaults.get() shouldBe 0
        }
    }

    "CURRENT BEHAVIOUR: the override lambda runs at create, before anything is requested, once per kontainer" {

        class Dyn : Counted()

        val runs = AtomicInteger()

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        val subject = blueprint.create { with { runs.incrementAndGet(); Dyn() } }

        assertSoftly {
            withClue("eager: ran although the dynamic was never requested") {
                runs.get() shouldBe 1
            }

            subject.get(Dyn::class)
            subject.get(Dyn::class)
            runs.get() shouldBe 1

            blueprint.create { with { runs.incrementAndGet(); Dyn() } }
            runs.get() shouldBe 2
        }
    }

    "A dynamic without an override: reported as Dynamic, built lazily from its definition, once per kontainer" {

        class Dyn : Counted()

        Births.reset()

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            Births.of(Dyn::class) shouldBe 0

            one.get(Dyn::class) shouldBeSameInstanceAs one.get(Dyn::class)
            one.get(Dyn::class) shouldNotBeSameInstanceAs two.get(Dyn::class)
            Births.of(Dyn::class) shouldBe 2

            one.typeOf(Dyn::class) shouldBe Type.Dynamic
        }
    }

    "Two kontainers from one blueprint with different overrides: each sees its own, also through a SemiDynamic" {

        class Dyn(val value: Int)
        class Holder(val dyn: Dyn) : Counted()

        Births.reset()

        val blueprint = kontainer {
            dynamic(Dyn::class) { Dyn(0) }
            singleton(Holder::class)
        }

        val one = blueprint.create { with { Dyn(1) } }
        val two = blueprint.create { with { Dyn(2) } }
        val three = blueprint.create()

        assertSoftly {
            one.get(Holder::class).dyn.value shouldBe 1
            two.get(Holder::class).dyn.value shouldBe 2
            three.get(Holder::class).dyn.value shouldBe 0

            one.typeOf(Dyn::class) shouldBe Type.DynamicOverride
            three.typeOf(Dyn::class) shouldBe Type.Dynamic

            Births.of(Holder::class) shouldBe 3
        }
    }

    "An override with a subclass of the dynamic is accepted" {

        open class Dyn
        class Sub : Dyn()

        val subject = kontainer {
            dynamic(Dyn::class)
        }.create {
            with { Sub() }
        }

        assertSoftly {
            subject.get(Dyn::class)::class shouldBe Sub::class
            subject.typeOf(Dyn::class) shouldBe Type.DynamicOverride
        }
    }

    "An override for a singleton is refused with KontainerInconsistent naming the class" {

        class Single

        val blueprint = kontainer {
            singleton(Single::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Single() } }
        }

        assertSoftly {
            error.message!! shouldStartWith "Unexpected dynamics were provided: "
            error.message shouldBe "Unexpected dynamics were provided: ${Single::class.getName()}"
        }
    }

    "An override for a prototype is refused with KontainerInconsistent" {

        class Proto

        val blueprint = kontainer {
            prototype(Proto::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Proto() } }
        }

        error.message!! shouldStartWith "Unexpected dynamics were provided: "
    }

    "An override for a SemiDynamic is refused with KontainerInconsistent" {

        class Dyn
        class Promoted(val dyn: Dyn)

        val blueprint = kontainer {
            dynamic(Dyn::class)
            singleton(Promoted::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Promoted(Dyn()) } }
        }

        error.message!! shouldStartWith "Unexpected dynamics were provided: "
    }

    "An override for a class that is not registered at all is refused with KontainerInconsistent" {

        class Dyn
        class Unknown

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Unknown() } }
        }

        error.message!! shouldStartWith "Unexpected dynamics were provided: "
    }

    "An override for a super type of the dynamic is refused with KontainerInconsistent" {

        open class Base
        class Dyn : Base()

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Base() } }
        }

        error.message!! shouldStartWith "Unexpected dynamics were provided: "
    }

    "The dynamics check runs on every create, not only the first" {

        class Single

        val blueprint = kontainer {
            singleton(Single::class)
        }

        blueprint.create()

        shouldThrow<KontainerInconsistent> {
            blueprint.create { with { Single() } }
        }
    }

    "An override that matches two dynamics is refused with ServiceAmbiguous" {

        open class Outer
        class Inner : Outer()

        val blueprint = kontainer {
            dynamic(Outer::class)
            dynamic(Inner::class)
        }

        shouldThrow<ServiceAmbiguous> {
            blueprint.create { with { Inner() } }
        }
    }

    "Cloning keeps the overrides and runs their lambdas again" {

        class Dyn : Counted()
        class Holder(val dyn: Dyn) : Counted()

        val runs = AtomicInteger()

        val original = kontainer {
            dynamic(Dyn::class)
            singleton(Holder::class)
        }.create {
            with { runs.incrementAndGet(); Dyn() }
        }

        val clone = original.clone()

        assertSoftly {
            runs.get() shouldBe 2
            clone.typeOf(Dyn::class) shouldBe Type.DynamicOverride
            clone.get(Dyn::class) shouldNotBeSameInstanceAs original.get(Dyn::class)
            clone.get(Holder::class) shouldNotBeSameInstanceAs original.get(Holder::class)
        }
    }

    "CURRENT BEHAVIOUR: cloning with an override for a non-dynamic skips the dynamics check and throws ServiceNotFound" {

        class Single

        val original = kontainer {
            singleton(Single::class)
        }.create()

        val error = shouldThrow<ServiceNotFound> {
            original.clone { with { Single() } }
        }

        error.message shouldContain "was not found"
    }
})
