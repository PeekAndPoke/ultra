package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceAmbiguous
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

open class UserProvider
open class AdminUserProvider : UserProvider()
class SuperAdmin : AdminUserProvider()
class NeedsUserProvider(val users: UserProvider)

open class Mailer
class SmtpMailer : Mailer()

/**
 * Pins the exact override match (ULTRA-00345): an override whose class is itself a registered dynamic replaces that
 * service; otherwise the one registered dynamic it fits, as before.
 */
class ExactOverrideMatchSpec : StringSpec({

    val blueprint = kontainer {
        dynamic(UserProvider::class)
        dynamic(AdminUserProvider::class)
    }

    /** The provider registered under exactly [cls]; `get(UserProvider::class)` itself is ambiguous here. */
    fun Kontainer.providerOf(cls: KClass<*>) = getServiceProviderFactory().getProvider(cls)

    fun Kontainer.valueOf(cls: KClass<*>) = providerOf(cls).provide(this, InjectionContext.kontainerRoot)

    "with { AdminUserProvider() } replaces AdminUserProvider and leaves UserProvider as it is" {

        val given = AdminUserProvider()
        val subject = blueprint.create { with { given } }

        assertSoftly {
            subject.providerOf(AdminUserProvider::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.get(AdminUserProvider::class) shouldBeSameInstanceAs given

            subject.providerOf(UserProvider::class).reported shouldBe Reported.DynamicSingleton
            subject.valueOf(UserProvider::class)::class shouldBe UserProvider::class

            withClue("reads through the base type are unchanged") {
                shouldThrow<ServiceAmbiguous> { subject.get(UserProvider::class) }
            }
        }
    }

    "with(AdminUserProvider::class) { ... } replaces AdminUserProvider and leaves UserProvider as it is" {

        val given = AdminUserProvider()
        val subject = blueprint.create { with(AdminUserProvider::class) { given } }

        assertSoftly {
            subject.providerOf(AdminUserProvider::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.get(AdminUserProvider::class) shouldBeSameInstanceAs given

            subject.providerOf(UserProvider::class).reported shouldBe Reported.DynamicSingleton
            subject.valueOf(UserProvider::class)::class shouldBe UserProvider::class
        }
    }

    "with(UserProvider::class) { AdminUserProvider() } replaces UserProvider: the key decides, not the instance" {

        val given = AdminUserProvider()
        val subject = blueprint.create { with(UserProvider::class) { given } }

        assertSoftly {
            subject.providerOf(UserProvider::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.valueOf(UserProvider::class) shouldBeSameInstanceAs given

            subject.providerOf(AdminUserProvider::class).reported shouldBe Reported.DynamicSingleton
            subject.get(AdminUserProvider::class) shouldNotBeSameInstanceAs given
        }
    }

    "An unregistered subclass of both stays ambiguous, with today's message" {

        shouldThrow<ServiceAmbiguous> {
            blueprint.create { with { SuperAdmin() } }
        }.message shouldBe "Service io.peekandpoke.ultra.kontainer.e2e.SuperAdmin is ambiguous. " +
                "It has multiple candidates: io.peekandpoke.ultra.kontainer.e2e.UserProvider, " +
                "io.peekandpoke.ultra.kontainer.e2e.AdminUserProvider"
    }

    "At clone: the exact match replaces AdminUserProvider, and an unregistered subclass stays ambiguous" {

        val original = blueprint.create()
        val given = AdminUserProvider()
        val clone = original.clone { with { given } }

        assertSoftly {
            clone.get(AdminUserProvider::class) shouldBeSameInstanceAs given
            clone.providerOf(UserProvider::class).reported shouldBe Reported.DynamicSingleton

            shouldThrow<ServiceAmbiguous> {
                original.clone { with { SuperAdmin() } }
            }
        }
    }

    "A clone replaces its parent's override by the exact class, and keeps the parent's other override" {

        val parentUser = UserProvider()
        val cloneAdmin = AdminUserProvider()

        val original = blueprint.create {
            with(UserProvider::class) { parentUser }
            with { AdminUserProvider() }
        }

        val clone = original.clone { with { cloneAdmin } }

        assertSoftly {
            clone.get(AdminUserProvider::class) shouldBeSameInstanceAs cloneAdmin
            clone.valueOf(UserProvider::class) shouldBeSameInstanceAs parentUser
        }
    }

    "The registration order does not matter: with the derived class registered first, the exact match still wins" {

        val given = AdminUserProvider()

        val subject = kontainer {
            dynamic(AdminUserProvider::class)
            dynamic(UserProvider::class)
        }.create {
            with { given }
        }

        assertSoftly {
            subject.providerOf(AdminUserProvider::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.get(AdminUserProvider::class) shouldBeSameInstanceAs given
            subject.providerOf(UserProvider::class).reported shouldBe Reported.DynamicSingleton
        }
    }

    "Only overridable services count as exact: a subclass registered as a singleton overrides its dynamic base" {

        val given = SmtpMailer()

        val subject = kontainer {
            dynamic(Mailer::class)
            singleton(SmtpMailer::class)
        }.create {
            with { given }
        }

        assertSoftly {
            subject.providerOf(Mailer::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.valueOf(Mailer::class) shouldBeSameInstanceAs given
            subject.providerOf(SmtpMailer::class).reported shouldBe Reported.StaticSingleton
            subject.get(SmtpMailer::class) shouldNotBeSameInstanceAs given
        }
    }

    "Only overridable services count as exact: a subclass registered as a dynamic prototype overrides its base" {

        val given = SmtpMailer()

        val subject = kontainer {
            dynamic(Mailer::class)
            dynamicPrototype(SmtpMailer::class)
        }.create {
            with { given }
        }

        assertSoftly {
            subject.providerOf(Mailer::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.valueOf(Mailer::class) shouldBeSameInstanceAs given
            subject.providerOf(SmtpMailer::class).reported shouldBe Reported.DynamicPrototype
            subject.get(SmtpMailer::class) shouldNotBeSameInstanceAs given
        }
    }

    "Primitives: with Int and Number registered, with<Int> replaces Int, by class equality" {

        val subject = kontainer {
            dynamic(Int::class)
            dynamic(Number::class) { 0.5 }
        }.create {
            with<Int> { 5 }
        }

        assertSoftly {
            subject.providerOf(Int::class).reported shouldBe Reported.OverriddenDynamicSingleton
            subject.providerOf(Number::class).reported shouldBe Reported.DynamicSingleton
        }
    }

    "Primitives: with Int alone registered, with { 5 } is refused as unexpected, the checker asks Java assignability" {

        shouldThrow<KontainerInconsistent> {
            kontainer { dynamic(Int::class) }.create { with { 5 } }
        }.message shouldBe "Unexpected dynamics were provided: kotlin.Int"
    }

    "Injection is not changed: injecting the base type stays ambiguous" {

        shouldThrow<KontainerInconsistent> {
            kontainer {
                dynamic(UserProvider::class)
                dynamic(AdminUserProvider::class)
                singleton(NeedsUserProvider::class)
            }.create()
        }.message shouldContain "Parameter 'users' is ambiguous. The following services collide: " +
                "io.peekandpoke.ultra.kontainer.e2e.UserProvider, io.peekandpoke.ultra.kontainer.e2e.AdminUserProvider"
    }
})
