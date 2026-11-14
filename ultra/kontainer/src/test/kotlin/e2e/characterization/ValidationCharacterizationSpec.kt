package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InvalidClassProvided
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.lang.reflect.InvocationTargetException

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

    "CURRENT BEHAVIOUR: a cycle through List is not reported and overflows the stack at get" {

        val subject = kontainer {
            singleton(ListCycleOne::class)
            singleton(ListCycleTwo::class)
        }.create()

        shouldThrow<StackOverflowError> {
            subject.get(ListCycleOne::class)
        }
    }

    "CURRENT BEHAVIOUR: a decorator with a nullable parameter of its own super type is reported as a cycle" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(RealRepo::class)
                singleton(DecoratingRepo::class)
            }.create()
        }

        error.message shouldContain "Circular dependency detected: " +
                "${DecoratingRepo::class.getName()} -> ${DecoratingRepo::class.getName()}"
    }

    "CURRENT BEHAVIOUR: after a failed validation the next create succeeds, as validation runs only once" {

        class Missing
        class Holder(val missing: Missing)

        val blueprint = kontainer { singleton(Holder::class) }

        shouldThrow<KontainerInconsistent> { blueprint.create() }

        val subject = shouldNotThrowAny { blueprint.create() }

        val thrown = shouldThrowAny { subject.get(Holder::class) }

        withClue("the missing dependency surfaces at get, as a reflection error of the constructor call") {
            thrown::class shouldBe InvocationTargetException::class
        }
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

abstract class Repo
class RealRepo : Repo()
class DecoratingRepo(val inner: Repo?) : Repo()
