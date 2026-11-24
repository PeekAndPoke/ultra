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
import io.peekandpoke.ultra.kontainer.DynamicOverrides
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceAmbiguous
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reportedOf
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins what `create { with { } }` does: overrides of dynamics, dynamics without one, and overrides that are refused.
 */
class DynamicsAtCreateCharacterizationSpec : StringSpec({

    "An override replaces the dynamic: reported as an overridden dynamic singleton, the default producer never runs" {

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
            subject.reportedOf(Dyn::class) shouldBe Reported.OverriddenDynamicSingleton
            subject.reportedOf(Holder::class) shouldBe Reported.SemiDynamicSingleton

            subject.get(Dyn::class) shouldBeSameInstanceAs given
            subject.get(Holder::class).dyn shouldBeSameInstanceAs given

            defaults.get() shouldBe 0
        }
    }

    // ULTRA-00344: overrides run at create (decided; the laziness of ULTRA-00333 is reverted).
    "The override lambda runs at create, once per kontainer, even when the service is never requested" {

        class Dyn : Counted()

        val runs = AtomicInteger()

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        val subject = blueprint.create { with { runs.incrementAndGet(); Dyn() } }

        assertSoftly {
            withClue("eager: ran at create, before anything was requested") {
                runs.get() shouldBe 1
            }

            subject.get(Dyn::class)
            subject.get(Dyn::class)
            runs.get() shouldBe 1

            blueprint.create { with { runs.incrementAndGet(); Dyn() } }
            runs.get() shouldBe 2
        }
    }

    "An override that is never requested counts one creation in its provider, the debug info and the dump" {

        class Dyn

        val subject = kontainer {
            dynamic(Dyn::class)
        }.create { with { Dyn() } }

        assertSoftly {
            subject.getProvider(Dyn::class).created.count shouldBe 1L
            subject.tools.getDebugInfo().services
                .first { it.cls.fqn == Dyn::class.java.name }.created.count shouldBe 1L
            subject.tools.dumpKontainer().lines()
                .first { it.startsWith(Dyn::class.getName() + " ") } shouldContain "| 1, first "
        }
    }

    "An override that throws fails create with its own exception, and no kontainer results" {

        class Dyn

        val blueprint = kontainer {
            dynamic(Dyn::class)
        }

        var subject: Kontainer? = null

        shouldThrow<IllegalStateException> {
            subject = blueprint.create { with<Dyn> { error("no override today") } }
        }.message shouldBe "no override today"

        assertSoftly {
            subject shouldBe null
            withClue("nothing is remembered: the next create with a working override succeeds") {
                blueprint.create { with { Dyn() } }.get(Dyn::class)::class shouldBe Dyn::class
            }
        }
    }

    "An override that throws fails clone with its own exception" {

        class Dyn

        val runs = AtomicInteger()

        val original = kontainer {
            dynamic(Dyn::class)
        }.create {
            with<Dyn> { if (runs.incrementAndGet() > 1) error("no second override") else Dyn() }
        }

        shouldThrow<IllegalStateException> {
            original.clone()
        }.message shouldBe "no second override"
    }

    "A refused override's lambda never runs, at create and at clone" {

        class Single
        class Dyn

        val runs = AtomicInteger()

        val blueprint = kontainer {
            singleton(Single::class)
            dynamic(Dyn::class)
        }

        assertSoftly {
            shouldThrow<KontainerInconsistent> {
                blueprint.create { with { runs.incrementAndGet(); Single() } }
            }

            shouldThrow<KontainerInconsistent> {
                blueprint.create { with<Kontainer> { runs.incrementAndGet(); blueprint.create() } }
            }

            shouldThrow<KontainerInconsistent> {
                blueprint.create().clone { with { runs.incrementAndGet(); Single() } }
            }

            runs.get() shouldBe 0
        }
    }

    "An override reads mutable state at create" {

        class Dyn(val value: Int)

        var current = 1

        val subject = kontainer {
            dynamic(Dyn::class) { Dyn(0) }
        }.create { with { Dyn(current) } }

        current = 2

        subject.get(Dyn::class).value shouldBe 1
    }

    "An override's definition names the instance's class, right from create" {

        open class Base
        class Derived : Base()

        val subject = kontainer {
            dynamic(Base::class)
        }.create { with(Base::class) { Derived() } }

        subject.tools.getDebugInfo().services
            .first { it.cls.fqn == Base::class.java.name }.definition.creates.fqn shouldBe Derived::class.java.name
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

            one.reportedOf(Dyn::class) shouldBe Reported.DynamicSingleton
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

            one.reportedOf(Dyn::class) shouldBe Reported.OverriddenDynamicSingleton
            three.reportedOf(Dyn::class) shouldBe Reported.DynamicSingleton

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
            subject.reportedOf(Dyn::class) shouldBe Reported.OverriddenDynamicSingleton
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

        error.message shouldBe "Unexpected dynamics were provided: ${Proto::class.getName()}"
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

    // An unregistered subclass of two dynamics: no exact match, so ambiguous (ULTRA-00345)
    "An override that matches two dynamics is refused with ServiceAmbiguous before any override lambda runs" {

        open class Outer
        open class Inner : Outer()
        class Leaf : Inner()
        class Other

        val blueprint = kontainer {
            dynamic(Outer::class)
            dynamic(Inner::class)
            dynamic(Other::class)
        }

        val validRuns = AtomicInteger()
        val ambiguousRuns = AtomicInteger()

        val error = shouldThrow<ServiceAmbiguous> {
            blueprint.create {
                with { validRuns.incrementAndGet(); Other() }
                with { ambiguousRuns.incrementAndGet(); Leaf() }
            }
        }

        assertSoftly {
            error.message shouldBe "Service Leaf is ambiguous. It has multiple candidates: Outer, Inner"
            validRuns.get() shouldBe 0
            ambiguousRuns.get() shouldBe 0
        }
    }

    "A clone adding an override that matches two dynamics is refused before the parent's lambdas run again" {

        open class Outer
        open class Inner : Outer()
        class Leaf : Inner()
        class Other

        val validRuns = AtomicInteger()
        val ambiguousRuns = AtomicInteger()

        val original = kontainer {
            dynamic(Outer::class)
            dynamic(Inner::class)
            dynamic(Other::class)
        }.create {
            with { validRuns.incrementAndGet(); Other() }
        }

        shouldThrow<ServiceAmbiguous> {
            original.clone { with { ambiguousRuns.incrementAndGet(); Leaf() } }
        }

        assertSoftly {
            withClue("only the parent's create ran the parent's lambda") {
                validRuns.get() shouldBe 1
            }
            ambiguousRuns.get() shouldBe 0
        }
    }

    "Override lambdas run in the order of their first with per service; a second with replaces in place" {

        class DynA
        class DynB
        class DynC

        val log = mutableListOf<String>()

        val blueprint = kontainer {
            dynamic(DynA::class)
            dynamic(DynB::class)
            dynamic(DynC::class)
        }

        val original = blueprint.create {
            with { log.add("A1"); DynA() }
            with { log.add("B"); DynB() }
            with { log.add("A2"); DynA() }
        }

        assertSoftly {
            withClue("A2 replaces A1 at A's position; A1 never runs") {
                log shouldBe listOf("A2", "B")
            }

            log.clear()
            original.clone {
                with { log.add("C"); DynC() }
                with { log.add("A3"); DynA() }
            }

            withClue("a clone runs the parent's order first, its own override for A at A's position, new keys last") {
                log shouldBe listOf("A3", "B", "C")
            }
        }
    }

    "Two overrides under different classes for two registered services are two overrides (ULTRA-00345)" {

        open class Base
        class Derived : Base()

        val log = mutableListOf<String>()

        val baseGiven = Base()
        val derivedGiven = Derived()

        val subject = kontainer {
            dynamic(Base::class)
            dynamic(Derived::class)
        }.create {
            with(Base::class) { log.add("base"); baseGiven }
            with(Derived::class) { log.add("derived"); derivedGiven }
        }

        val factory = subject.getServiceProviderFactory()

        assertSoftly {
            log shouldBe listOf("base", "derived")
            factory.getProvider(Base::class).provide(subject, InjectionContext.kontainerRoot) shouldBeSameInstanceAs
                    baseGiven
            subject.get(Derived::class) shouldBeSameInstanceAs derivedGiven
        }
    }

    // Decided to stay (ULTRA-00345): an edge case, the user should be careful.
    "Two overrides for one service under different classes both run, the later one wins" {

        open class Base
        class First : Base()
        class Derived : Base()

        val log = mutableListOf<String>()

        val subject = kontainer {
            dynamic(Base::class)
        }.create {
            with(Base::class) { log.add("first"); First() }
            with(Derived::class) { log.add("derived"); Derived() }
        }

        assertSoftly {
            log shouldBe listOf("first", "derived")
            subject.get(Base::class)::class shouldBe Derived::class
        }
    }

    // Decided to stay (ULTRA-00345), the clone flavour of the case above.
    "A clone's override under the base class runs, but the parent's later override under a subclass wins" {

        open class Base
        class Sub : Base()

        val log = mutableListOf<String>()

        val original = kontainer {
            dynamic(Base::class)
        }.create {
            with(Base::class) { log.add("a"); Base() }
            with(Sub::class) { log.add("b"); Sub() }
        }

        log.clear()
        val clone = original.clone { with(Base::class) { log.add("x"); Base() } }

        assertSoftly {
            withClue("merged order: Base with the clone's lambda, then Sub with the parent's") {
                log shouldBe listOf("x", "b")
            }
            clone.get(Base::class)::class shouldBe Sub::class
            clone.get(Base::class) shouldNotBeSameInstanceAs original.get(Base::class)
        }
    }

    "Cloning keeps the overrides and runs their lambdas once more for the clone, at clone" {

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
            clone.reportedOf(Dyn::class) shouldBe Reported.OverriddenDynamicSingleton
            clone.get(Dyn::class) shouldNotBeSameInstanceAs original.get(Dyn::class)
            clone.get(Holder::class) shouldNotBeSameInstanceAs original.get(Holder::class)
            runs.get() shouldBe 2
        }
    }

    // ULTRA-00353: the overrides are copied when the builder is built.
    "A builder kept after create and used again does not change what later clones of that kontainer get" {

        open class Dyn
        class First : Dyn()
        class Second : Dyn()

        var kept: DynamicOverrides.Builder? = null

        val original = kontainer {
            dynamic(Dyn::class)
        }.create {
            with(Dyn::class) { First() }
            kept = this
        }

        kept!!.with(Dyn::class) { Second() }

        assertSoftly {
            original.get(Dyn::class)::class shouldBe First::class
            original.clone().get(Dyn::class)::class shouldBe First::class
        }
    }

    // ULTRA-00332: clone runs the same check as create.
    "Cloning with an override for a non-dynamic is refused like create: KontainerInconsistent, the same message" {

        class Single

        val original = kontainer {
            singleton(Single::class)
        }.create()

        val error = shouldThrow<KontainerInconsistent> {
            original.clone { with { Single() } }
        }

        error.message shouldBe "Unexpected dynamics were provided: ${Single::class.getName()}"
    }

    // ULTRA-00336: the built-in services cannot be overridden.
    "An override for the Kontainer itself is refused: it is managed by the kontainer itself" {

        class UsesKontainer(val kontainer: Kontainer)

        val blueprint = kontainer {
            singleton(UsesKontainer::class)
        }

        val foreign = blueprint.create()

        shouldThrow<KontainerInconsistent> {
            blueprint.create { with<Kontainer> { foreign } }
        }.message shouldBe
                "io.peekandpoke.ultra.kontainer.Kontainer is managed by the kontainer itself and cannot be overridden"
    }

})
