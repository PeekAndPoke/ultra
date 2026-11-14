package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reportedOf
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module

/**
 * Pins what a later definition of the same service does to its type, its producer and the promotion of its users.
 */
class RedefinitionCharacterizationSpec : StringSpec({

    "Singleton redefined as dynamic: Dynamic, and a singleton injecting it becomes SemiDynamic" {

        class Target : Counted()
        class User(val target: Target) : Counted()

        val blueprint = kontainer {
            singleton(Target::class)
            singleton(User::class)
            dynamic(Target::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            one.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)
        }
    }

    "Dynamic redefined as singleton: stays Dynamic, uses the later producer, users become SemiDynamic" {

        abstract class Target
        class First : Target()
        class Second : Target()
        class User(val target: Target) : Counted()

        val blueprint = kontainer {
            dynamic(Target::class, First::class)
            singleton(User::class)
            singleton(Target::class, Second::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            one.get(Target::class)::class shouldBe Second::class
            one.get(Target::class) shouldNotBeSameInstanceAs two.get(Target::class)
            one.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton

            blueprint.create { with<Target> { First() } }.reportedOf(Target::class) shouldBe Reported.OverriddenDynamicSingleton
        }
    }

    "Singleton redefined as prototype: Prototype, and a singleton injecting it stays global" {

        class Target : Counted()
        class User(val target: Target) : Counted()

        val blueprint = kontainer {
            singleton(Target::class)
            singleton(User::class)
            prototype(Target::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Target::class) shouldBe Reported.StaticPrototype
            one.get(Target::class) shouldNotBeSameInstanceAs one.get(Target::class)
            one.reportedOf(User::class) shouldBe Reported.StaticSingleton
            one.get(User::class) shouldBeSameInstanceAs two.get(User::class)
        }
    }

    "Prototype redefined as singleton: stays Prototype and uses the later producer" {

        abstract class Target
        class First : Target()
        class Second : Target()

        val subject = kontainer {
            prototype(Target::class, First::class)
            singleton(Target::class, Second::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Target::class) shouldBe Reported.StaticPrototype
            subject.get(Target::class)::class shouldBe Second::class
            subject.get(Target::class) shouldNotBeSameInstanceAs subject.get(Target::class)
        }
    }

    // ULTRA-00331: the first definition fixes the type, a dynamic one adds the binding.
    "Prototype redefined as dynamic: Prototype.Dynamic, its users are promoted, an override at create is refused" {

        class Target : Counted()
        class User(val target: Target) : Counted()

        val blueprint = kontainer {
            prototype(Target::class)
            singleton(User::class)
            dynamic(Target::class)
        }

        val one = blueprint.create()
        val two = blueprint.create()

        assertSoftly {
            one.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
            one.get(Target::class) shouldNotBeSameInstanceAs one.get(Target::class)
            one.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)

            shouldThrow<KontainerInconsistent> {
                blueprint.create { with { Target() } }
            }.message shouldBe "Unexpected dynamics were provided: ${Target::class.getName()}" +
                    " (only dynamic singletons can be overridden, not the dynamic prototype ${Target::class.getName()})"
        }
    }

    "Dynamic redefined as prototype: stays Dynamic" {

        class Target : Counted()

        val subject = kontainer {
            dynamic(Target::class)
            prototype(Target::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            subject.get(Target::class) shouldBeSameInstanceAs subject.get(Target::class)
        }
    }

    "The order of redefinitions decides: singleton, prototype, dynamic ends as Prototype.Dynamic (ULTRA-00331)" {

        class Target

        val subject = kontainer {
            singleton(Target::class)
            prototype(Target::class)
            dynamic(Target::class)
        }.create()

        subject.reportedOf(Target::class) shouldBe Reported.DynamicPrototype
    }

    "The order of redefinitions decides: singleton, dynamic, prototype ends as Dynamic" {

        class Target

        val subject = kontainer {
            singleton(Target::class)
            dynamic(Target::class)
            prototype(Target::class)
        }.create()

        subject.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
    }

    "Promotion uses the parameters of the final definition only: a redefinition without the dynamic stays global" {

        class Dyn : Counted()
        class Target(val label: String)

        val withDynamicFirst = kontainer {
            dynamic(Dyn::class)
            singleton(Target::class) { _: Dyn -> Target("first") }
            singleton(Target::class) { Target("second") }
        }.create()

        val withDynamicLast = kontainer {
            dynamic(Dyn::class)
            singleton(Target::class) { Target("first") }
            singleton(Target::class) { _: Dyn -> Target("second") }
        }.create()

        assertSoftly {
            withDynamicFirst.reportedOf(Target::class) shouldBe Reported.StaticSingleton
            withDynamicFirst.get(Target::class).label shouldBe "second"

            withDynamicLast.reportedOf(Target::class) shouldBe Reported.SemiDynamicSingleton
            withDynamicLast.get(Target::class).label shouldBe "second"
        }
    }

    "Redefinition across modules: the later module wins and the type upgrade applies" {

        class Target : Counted()
        class User(val target: Target) : Counted()

        val base = module {
            singleton(Target::class)
            singleton(User::class)
        }

        val overriding = module {
            dynamic(Target::class)
        }

        val forward = kontainer {
            module(base)
            module(overriding)
        }.create()

        val backward = kontainer {
            module(overriding)
            module(base)
        }.create()

        assertSoftly {
            forward.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            forward.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton

            backward.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            backward.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
        }
    }

    // ULTRA-00330: extend applies the redefinition rule.
    "Extend applies the redefinition rule: a prototype redefined as singleton stays Prototype" {

        class Target

        val base = kontainer {
            prototype(Target::class)
        }

        val extended = base.extend {
            singleton(Target::class)
        }

        assertSoftly {
            base.create().reportedOf(Target::class) shouldBe Reported.StaticPrototype
            extended.create().reportedOf(Target::class) shouldBe Reported.StaticPrototype
        }
    }

    // ULTRA-00330: extend applies the redefinition rule.
    "Extend applies the redefinition rule: a dynamic redefined as singleton stays Dynamic, the promotion stays" {

        class Target
        class User(val target: Target)

        val base = kontainer {
            dynamic(Target::class)
            singleton(User::class)
        }

        val extended = base.extend {
            singleton(Target::class)
        }

        val one = extended.create()
        val two = extended.create()

        assertSoftly {
            base.create().reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton

            one.reportedOf(Target::class) shouldBe Reported.DynamicSingleton
            one.reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
            one.get(User::class) shouldNotBeSameInstanceAs two.get(User::class)
        }
    }

    "Extend applies the redefinition rule like one builder: prototype in the base, dynamic in the extension" {

        class Target

        val extended = kontainer {
            prototype(Target::class)
        }.extend {
            dynamic(Target::class)
        }

        extended.create().reportedOf(Target::class) shouldBe Reported.DynamicPrototype
    }

    "Extend applies the redefinition rule like one builder: a static singleton in the base is replaced as is" {

        class Target

        val extended = kontainer {
            singleton(Target::class)
        }.extend {
            prototype(Target::class)
        }

        extended.create().reportedOf(Target::class) shouldBe Reported.StaticPrototype
    }

    "Extend with a dynamic promotes singletons of the base blueprint" {

        class Target
        class User(val target: Target)

        val base = kontainer {
            singleton(Target::class)
            singleton(User::class)
        }

        val extended = base.extend {
            dynamic(Target::class)
        }

        assertSoftly {
            base.create().reportedOf(User::class) shouldBe Reported.StaticSingleton
            extended.create().reportedOf(User::class) shouldBe Reported.SemiDynamicSingleton
        }
    }
})
