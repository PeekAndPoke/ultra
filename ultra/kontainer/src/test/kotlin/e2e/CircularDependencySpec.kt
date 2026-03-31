package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.kontainer

// Test domain classes for circular dependency detection

class CircA(val b: CircB)
class CircB(val a: CircA)

class ChainA(val b: ChainB)
class ChainB(val c: ChainC)
class ChainC(val a: ChainA)

class LazyCircA(val b: Lazy<LazyCircB>)
class LazyCircB(val a: LazyCircA)

class NoCycleA(val b: NoCycleB)
class NoCycleB

class CircularDependencySpec : StringSpec({

    "Direct circular dependency A <-> B must be detected" {
        val blueprint = kontainer {
            singleton(CircA::class)
            singleton(CircB::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create()
        }

        error.message shouldContain "Circular dependency detected"
        error.message shouldContain "CircA"
        error.message shouldContain "CircB"
        error.message shouldContain "Lazy<T>"
    }

    "Transitive circular dependency A -> B -> C -> A must be detected" {
        val blueprint = kontainer {
            singleton(ChainA::class)
            singleton(ChainB::class)
            singleton(ChainC::class)
        }

        val error = shouldThrow<KontainerInconsistent> {
            blueprint.create()
        }

        error.message shouldContain "Circular dependency detected"
    }

    "Lazy injection must break cycles — no error expected" {
        val blueprint = kontainer {
            singleton(LazyCircA::class)
            singleton(LazyCircB::class)
        }

        shouldNotThrow<KontainerInconsistent> {
            blueprint.create()
        }
    }

    "Non-circular dependencies must not trigger false positives" {
        val blueprint = kontainer {
            singleton(NoCycleA::class)
            singleton(NoCycleB::class)
        }

        shouldNotThrow<KontainerInconsistent> {
            blueprint.create()
        }
    }
})
