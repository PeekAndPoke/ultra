package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
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
            one.typeOf(Target::class) shouldBe Type.Dynamic
            one.typeOf(User::class) shouldBe Type.SemiDynamic
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
            one.typeOf(Target::class) shouldBe Type.Dynamic
            one.get(Target::class)::class shouldBe Second::class
            one.get(Target::class) shouldNotBeSameInstanceAs two.get(Target::class)
            one.typeOf(User::class) shouldBe Type.SemiDynamic

            blueprint.create { with<Target> { First() } }.typeOf(Target::class) shouldBe Type.DynamicOverride
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
            one.typeOf(Target::class) shouldBe Type.Prototype
            one.get(Target::class) shouldNotBeSameInstanceAs one.get(Target::class)
            one.typeOf(User::class) shouldBe Type.Singleton
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
            subject.typeOf(Target::class) shouldBe Type.Prototype
            subject.get(Target::class)::class shouldBe Second::class
            subject.get(Target::class) shouldNotBeSameInstanceAs subject.get(Target::class)
        }
    }

    "CURRENT BEHAVIOUR: prototype redefined as dynamic stays Prototype: no promotion of users, no override at create" {

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
            one.typeOf(Target::class) shouldBe Type.Prototype
            one.typeOf(User::class) shouldBe Type.Singleton
            one.get(User::class) shouldBeSameInstanceAs two.get(User::class)

            shouldThrow<KontainerInconsistent> {
                blueprint.create { with { Target() } }
            }.message!! shouldStartWith "Unexpected dynamics were provided: "
        }
    }

    "Dynamic redefined as prototype: stays Dynamic" {

        class Target : Counted()

        val subject = kontainer {
            dynamic(Target::class)
            prototype(Target::class)
        }.create()

        assertSoftly {
            subject.typeOf(Target::class) shouldBe Type.Dynamic
            subject.get(Target::class) shouldBeSameInstanceAs subject.get(Target::class)
        }
    }

    "The order of redefinitions decides: singleton, prototype, dynamic ends as Prototype" {

        class Target

        val subject = kontainer {
            singleton(Target::class)
            prototype(Target::class)
            dynamic(Target::class)
        }.create()

        subject.typeOf(Target::class) shouldBe Type.Prototype
    }

    "The order of redefinitions decides: singleton, dynamic, prototype ends as Dynamic" {

        class Target

        val subject = kontainer {
            singleton(Target::class)
            dynamic(Target::class)
            prototype(Target::class)
        }.create()

        subject.typeOf(Target::class) shouldBe Type.Dynamic
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
            withDynamicFirst.typeOf(Target::class) shouldBe Type.Singleton
            withDynamicFirst.get(Target::class).label shouldBe "second"

            withDynamicLast.typeOf(Target::class) shouldBe Type.SemiDynamic
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
            forward.typeOf(Target::class) shouldBe Type.Dynamic
            forward.typeOf(User::class) shouldBe Type.SemiDynamic

            backward.typeOf(Target::class) shouldBe Type.Dynamic
            backward.typeOf(User::class) shouldBe Type.SemiDynamic
        }
    }

    "CURRENT BEHAVIOUR: extend does not apply the type upgrade, a prototype redefined as singleton becomes Singleton" {

        class Target

        val base = kontainer {
            prototype(Target::class)
        }

        val extended = base.extend {
            singleton(Target::class)
        }

        assertSoftly {
            base.create().typeOf(Target::class) shouldBe Type.Prototype
            extended.create().typeOf(Target::class) shouldBe Type.Singleton
        }
    }

    "CURRENT BEHAVIOUR: extend does not apply the type upgrade, a dynamic redefined as singleton ends the promotion" {

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
            base.create().typeOf(User::class) shouldBe Type.SemiDynamic

            one.typeOf(Target::class) shouldBe Type.Singleton
            one.typeOf(User::class) shouldBe Type.Singleton
            one.get(User::class) shouldBeSameInstanceAs two.get(User::class)
        }
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
            base.create().typeOf(User::class) shouldBe Type.Singleton
            extended.create().typeOf(User::class) shouldBe Type.SemiDynamic
        }
    }
})
