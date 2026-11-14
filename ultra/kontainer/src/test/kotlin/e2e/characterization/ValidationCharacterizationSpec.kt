package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.DynamicOverrides
import io.peekandpoke.ultra.kontainer.InvalidClassProvided
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceProviderFactory
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the validation of a blueprint: when it runs, the exception type and the essential part of the message.
 */
class ValidationCharacterizationSpec : StringSpec({

    "Validation runs at the first create, not when the blueprint is built" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = shouldNotThrowAny {
            kontainer { singleton(Holder::class) }
        }

        shouldThrow<KontainerInconsistent> { blueprint.create() }
    }

    "Missing dependency: KontainerInconsistent naming the service and the parameter" {

        class Missing
        class Holder(val missing: Missing)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer { singleton(Holder::class) }.create()
        }

        assertSoftly {
            error.message!! shouldStartWith "Kontainer is inconsistent!\n\nProblems:\n\n"
            error.message shouldContain "1. Service '${Holder::class.getName()}'"
            error.message shouldContain "-> Parameter 'missing' misses a dependency to ${Missing::class.getName()}"
        }
    }

    "Missing dependency of a prototype and of a dynamic: the same error" {

        class Missing
        class Proto(val missing: Missing)
        class Dyn(val missing: Missing)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                prototype(Proto::class)
                dynamic(Dyn::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Service '${Proto::class.getName()}'"
            error.message shouldContain "Service '${Dyn::class.getName()}'"
            error.message shouldContain "1. Service"
            error.message shouldContain "2. Service"
        }
    }

    "Missing lazy dependency: KontainerInconsistent with the lazy wording" {

        class Missing
        class Holder(val missing: Lazy<Missing>)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer { singleton(Holder::class) }.create()
        }

        error.message shouldContain "Parameter 'missing' misses a lazy dependency to '${Missing::class.getName()}'"
    }

    "Missing services behind List, Lazy<List> and Lookup are no error: they inject empty" {

        abstract class Missing
        class Holder(val list: List<Missing>, val lazyList: Lazy<List<Missing>>, val lookup: Lookup<Missing>)

        val subject = kontainer { singleton(Holder::class) }.create()

        assertSoftly {
            subject.get(Holder::class).list shouldBe emptyList()
            subject.get(Holder::class).lazyList.value shouldBe emptyList()
            subject.get(Holder::class).lookup.all() shouldBe emptyList()
        }
    }

    "Ambiguous dependency: KontainerInconsistent naming the colliding services" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()
        class Holder(val base: Base)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(Holder::class)
                singleton(ImplOne::class)
                singleton(ImplTwo::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Parameter 'base' is ambiguous. The following services collide: "
            error.message shouldContain ImplOne::class.getName()
            error.message shouldContain ImplTwo::class.getName()
        }
    }

    "Ambiguous lazy dependency: the same wording" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()
        class Holder(val base: Lazy<Base>)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(Holder::class)
                dynamic(ImplOne::class)
                prototype(ImplTwo::class)
            }.create()
        }

        error.message shouldContain "Parameter 'base' is ambiguous. The following services collide: "
    }

    "A primitive or String parameter cannot be injected" {

        class Holder(val count: Int, val name: String)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer { singleton(Holder::class) }.create()
        }

        assertSoftly {
            error.message shouldContain "Parameter 'count' has no known way to inject a kotlin.Int"
            error.message shouldContain "Parameter 'name' has no known way to inject a kotlin.String"
        }
    }

    "An interface or abstract class as service fails when the blueprint is built" {

        abstract class Abstract

        val error = shouldThrow<InvalidClassProvided> {
            kontainer { singleton(Abstract::class) }
        }

        error.message shouldBe "A service cannot be an interface or abstract class"
    }

    "A cycle of two singletons: KontainerInconsistent with the path and the hint" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(CycleOne::class)
                singleton(CycleTwo::class)
            }.create()
        }

        assertSoftly {
            error.message!! shouldStartWith "Kontainer has circular dependencies!\n\n"
            error.message shouldContain "  - Circular dependency detected: " +
                    "${CycleOne::class.getName()} -> ${CycleTwo::class.getName()} -> ${CycleOne::class.getName()}"
            error.message shouldContain "Hint: Use Lazy<T> injection to break the cycle."
        }
    }

    "A cycle of two prototypes is detected" {

        shouldThrow<KontainerInconsistent> {
            kontainer {
                prototype(CycleOne::class)
                prototype(CycleTwo::class)
            }.create()
        }.message shouldContain "Circular dependency detected"
    }

    "A cycle through a dynamic is detected" {

        shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(CycleOne::class)
                dynamic(CycleTwo::class)
            }.create()
        }.message shouldContain "Circular dependency detected"
    }

    "A cycle through a nullable parameter is detected" {

        shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(NullableCycleOne::class)
                singleton(NullableCycleTwo::class)
            }.create()
        }.message shouldContain "Circular dependency detected"
    }

    "A cycle through a factory is detected" {

        shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(CycleOne::class) { two: CycleTwo -> CycleOne(two) }
                singleton(CycleTwo::class) { one: CycleOne -> CycleTwo(one) }
            }.create()
        }.message shouldContain "Circular dependency detected"
    }

    "The cycle check runs before the parameter check and hides its errors" {

        class Missing
        class Holder(val missing: Missing)

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(CycleOne::class)
                singleton(CycleTwo::class)
                singleton(Holder::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Circular dependency detected"
            error.message shouldNotContain "misses a dependency"
        }
    }

    "A cycle through Lookup is not reported and works, as the lookup is lazy" {

        val subject = kontainer {
            singleton(LookupCycleOne::class)
            singleton(LookupCycleTwo::class)
        }.create()

        val one = subject.get(LookupCycleOne::class)

        one.lookup.get(LookupCycleTwo::class).one shouldBe one
    }

    // ULTRA-00328: the cycle check follows every Direct injection, List included.
    "A cycle through List is a validation error with the cycle and the hint" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(ListCycleOne::class)
                singleton(ListCycleTwo::class)
            }.create()
        }

        assertSoftly {
            error.message!! shouldStartWith "Kontainer has circular dependencies!\n\n"
            error.message shouldContain "  - Circular dependency detected: " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleOne" +
                    " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleTwo" +
                    " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleOne"
            error.message shouldContain "Hint: Use Lazy<T> injection to break the cycle."
        }
    }

    "A cycle through a List with two candidates is a validation error too" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(ListOfMany::class)
                singleton(ManyCyclic::class)
                singleton(ManyPlain::class)
            }.create()
        }

        error.message shouldContain "Circular dependency detected: " +
                "io.peekandpoke.ultra.kontainer.e2e.characterization.ListOfMany" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ManyCyclic" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ListOfMany"
    }

    "A List cycle is found when one listed class also has a listed subclass" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(PartOwner::class)
                singleton(MidPart::class)
                singleton(LeafPart::class)
            }.create()
        }

        error.message shouldContain "Circular dependency detected: " +
                "io.peekandpoke.ultra.kontainer.e2e.characterization.PartOwner" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.MidPart" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.PartOwner"
    }

    "A composite that injects a List of its own super type is in its own list: a cycle with itself" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(CompositeHandler::class)
                singleton(PlainHandler::class)
            }.create()
        }

        error.message shouldContain "Circular dependency detected: " +
                "io.peekandpoke.ultra.kontainer.e2e.characterization.CompositeHandler" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.CompositeHandler"
    }

    "A composite with a Lazy List of its own super type boots, and its list contains the composite itself" {

        val subject = kontainer {
            singleton(LazyCompositeHandler::class)
            singleton(PlainHandler::class)
        }.create()

        val composite = subject.get(LazyCompositeHandler::class)

        assertSoftly {
            composite.all.value.size shouldBe 2
            composite.all.value.any { it === composite } shouldBe true
        }
    }

    "The checked path through the public ServiceProviderFactory validates an invalid blueprint that never ran create" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        val factory = ServiceProviderFactory(
            blueprint = blueprint,
            dynamics = DynamicOverrides(emptyMap()),
            providerProviders = emptyMap(),
        )

        shouldThrow<KontainerInconsistent> {
            factory.newKontainer(DynamicOverrides(emptyMap()))
        }.message!! shouldStartWith "Kontainer is inconsistent!"
    }

    "A List cycle is found whatever declares the owner: a factory, a dynamic, a prototype" {

        val expected = "Circular dependency detected: " +
                "io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleOne" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleTwo" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.ListCycleOne"

        assertSoftly {
            withClue("the owner registered by a factory") {
                shouldThrow<KontainerInconsistent> {
                    kontainer {
                        singleton(ListCycleOne::class) { all: List<ListCycleTwo> -> ListCycleOne(all) }
                        singleton(ListCycleTwo::class)
                    }.create()
                }.message shouldContain expected
            }

            withClue("the owner registered as a dynamic") {
                shouldThrow<KontainerInconsistent> {
                    kontainer {
                        dynamic(ListCycleOne::class)
                        singleton(ListCycleTwo::class)
                    }.create()
                }.message shouldContain expected
            }

            withClue("the owner registered as a prototype") {
                shouldThrow<KontainerInconsistent> {
                    kontainer {
                        prototype(ListCycleOne::class)
                        singleton(ListCycleTwo::class)
                    }.create()
                }.message shouldContain expected
            }
        }
    }

    "A cycle through Lazy<List> is not reported and works, as the list is lazy" {

        val subject = kontainer {
            singleton(LazyListCycleOne::class)
            singleton(LazyListCycleTwo::class)
        }.create()

        val one = subject.get(LazyListCycleOne::class)

        one.all.value.single().one shouldBe one
    }

    // ULTRA-00334 and ULTRA-00335: ambiguous, not a cycle.
    "A decorator with a nullable parameter of its own super type is reported as ambiguous, not as a cycle" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(RealRepo::class)
                singleton(DecoratingRepo::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Parameter 'inner' is ambiguous. The following services collide: " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.RealRepo" +
                    ", io.peekandpoke.ultra.kontainer.e2e.characterization.DecoratingRepo"
            error.message shouldNotContain "Circular dependency"
        }
    }

    // ULTRA-00327: the validation result is remembered.
    "After a failed validation every create throws the same error with the same message" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        val first = shouldThrow<KontainerInconsistent> { blueprint.create() }
        val second = shouldThrow<KontainerInconsistent> { blueprint.create() }
        val third = shouldThrow<KontainerInconsistent> { blueprint.create() }

        assertSoftly {
            first.message!! shouldStartWith "Kontainer is inconsistent!"
            second.message shouldBe first.message
            third.message shouldBe first.message
        }
    }

    "After a failed validation the override block of a later create does not run either" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        shouldThrow<KontainerInconsistent> { blueprint.create() }

        var ran = false
        shouldThrow<KontainerInconsistent> { blueprint.create { ran = true } }

        ran shouldBe false
    }

    "Concurrent first creates of an invalid blueprint: every one of them gets the validation error" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        val start = CountDownLatch(1)
        val outcomes = ConcurrentLinkedQueue<String>()

        val threads = (1..8).map {
            Thread {
                start.await()
                outcomes.add(
                    try {
                        blueprint.create()
                        "kontainer"
                    } catch (e: KontainerInconsistent) {
                        e.message!!
                    }
                )
            }.apply { start() }
        }

        start.countDown()
        threads.forEach { it.join(10_000) }

        assertSoftly {
            outcomes.size shouldBe 8
            outcomes.toSet().size shouldBe 1
            outcomes.first() shouldStartWith "Kontainer is inconsistent!"
        }
    }

    "Concurrent first creates of a valid blueprint: every one of them gets a kontainer" {

        class Single

        val blueprint = kontainer { singleton(Single::class) }

        val start = CountDownLatch(1)
        val created = AtomicInteger()

        val threads = (1..8).map {
            Thread {
                start.await()
                blueprint.create()
                created.incrementAndGet()
            }.apply { start() }
        }

        start.countDown()
        threads.forEach { it.join(10_000) }

        created.get() shouldBe 8
    }

    "Validation runs before the override block of the first create: the block does not run on an invalid blueprint" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        var ran = false

        shouldThrow<KontainerInconsistent> {
            blueprint.create { ran = true }
        }.message!! shouldStartWith "Kontainer is inconsistent!"

        ran shouldBe false
    }

    "Validation runs before the override block of the first create: a throwing block does not postpone it" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        shouldThrow<KontainerInconsistent> {
            blueprint.create { error("the block throws") }
        }.message!! shouldStartWith "Kontainer is inconsistent!"
    }

    // ULTRA-00334: ambiguous, not a cycle.
    "A decorator with a non-null parameter of its own super type is reported as ambiguous, not as a cycle" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(RealRepo::class)
                singleton(CachingRepo::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Parameter 'inner' is ambiguous. The following services collide: " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.RealRepo" +
                    ", io.peekandpoke.ultra.kontainer.e2e.characterization.CachingRepo"
            error.message shouldNotContain "Circular dependency"
        }
    }

    "An ambiguous injection on a cycle through another service is reported as ambiguous, and hidden errors show" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(AmbiguousOnCycle::class)
                singleton(CycleBackToA::class)
                singleton(OtherI::class)
            }.create()
        }

        assertSoftly {
            error.message shouldContain "Parameter 'i' is ambiguous. The following services collide: " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.CycleBackToA, " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.OtherI"
            error.message shouldContain "Parameter 'missing' misses a dependency to " +
                    "io.peekandpoke.ultra.kontainer.e2e.characterization.NeverRegistered"
            error.message shouldNotContain "Circular dependency"
        }
    }

    "A real self-cycle, the owner the only candidate, stays a cycle" {

        shouldThrow<KontainerInconsistent> {
            kontainer { singleton(SelfCycle::class) }.create()
        }.message shouldContain "Circular dependency detected: " +
                "io.peekandpoke.ultra.kontainer.e2e.characterization.SelfCycle" +
                " -> io.peekandpoke.ultra.kontainer.e2e.characterization.SelfCycle"
    }

    "A decorator naming the concrete class is valid and gets it injected" {

        val subject = kontainer {
            singleton(RealRepo::class)
            singleton(ConcreteCachingRepo::class)
        }.create()

        subject.get(ConcreteCachingRepo::class).inner shouldBe subject.get(RealRepo::class)
    }

})

class CycleOne(val two: CycleTwo)
class CycleTwo(val one: CycleOne)

class NullableCycleOne(val two: NullableCycleTwo?)
class NullableCycleTwo(val one: NullableCycleOne?)

class LookupCycleOne(val lookup: Lookup<LookupCycleTwo>)
class LookupCycleTwo(val one: LookupCycleOne)

class ListCycleOne(val all: List<ListCycleTwo>)
class ListCycleTwo(val one: ListCycleOne)

interface CycleI
class NeverRegistered
class AmbiguousOnCycle(val i: CycleI, val missing: NeverRegistered)
class CycleBackToA(val a: AmbiguousOnCycle) : CycleI
class OtherI : CycleI

abstract class Part
class PartOwner(val all: List<Part>)
open class MidPart(val owner: PartOwner) : Part()
class LeafPart(owner: PartOwner) : MidPart(owner)

abstract class HandlerBase
class CompositeHandler(val all: List<HandlerBase>) : HandlerBase()
class LazyCompositeHandler(val all: Lazy<List<HandlerBase>>) : HandlerBase()
class PlainHandler : HandlerBase()

abstract class ManyBase
class ListOfMany(val all: List<ManyBase>)
class ManyCyclic(val owner: ListOfMany) : ManyBase()
class ManyPlain : ManyBase()

class LazyListCycleOne(val all: Lazy<List<LazyListCycleTwo>>)
class LazyListCycleTwo(val one: LazyListCycleOne)

abstract class Repo
class RealRepo : Repo()
class DecoratingRepo(val inner: Repo?) : Repo()
class CachingRepo(val inner: Repo) : Repo()
class ConcreteCachingRepo(val inner: RealRepo) : Repo()
class SelfCycle(val self: SelfCycle)
