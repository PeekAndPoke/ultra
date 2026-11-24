package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InvalidClassProvided
import io.peekandpoke.ultra.kontainer.ServiceAmbiguous
import io.peekandpoke.ultra.kontainer.ServiceNotFound
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reported
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.IllegalCallableAccessException

class PathsUnknown
class PathsStatic
open class PathsDyn
class PathsDynOverride : PathsDyn()
class PathsSemi(val dyn: PathsDyn)
class PathsProto

interface PathsBase
open class PathsMiddle : PathsBase
class PathsLeaf : PathsMiddle()
class PathsOther : PathsBase
class PathsListHolder(val all: List<PathsBase>)
class PathsLazyListHolder(val all: Lazy<List<PathsBase>>)
class PathsLookupHolder(val all: Lookup<PathsBase>)

class PathsTwoNoArg() {
    @Suppress("unused", "UNUSED_PARAMETER")
    constructor(x: Int = 1) : this()
}

class PathsThrowingNoArg {
    init {
        error("no-arg constructor failed")
    }
}

class PathsThrowingWithArgs(@Suppress("unused") val static: PathsStatic) {
    init {
        error("constructor with arguments failed")
    }
}

class PathsPrivateNoArg private constructor()

class PathsPrivateWithArgs private constructor(@Suppress("unused") val static: PathsStatic)

object PathsObject

@JvmInline
value class PathsId(val value: String)

class PathsUsesId(val id: PathsId, val static: PathsStatic)

/**
 * Pins the provision paths ULTRA-00351 makes faster: the provider map of a kontainer, resolution and re-resolution,
 * and how a class producer builds its instances, its failures included.
 */
class ProvisionPathsCharacterizationSpec : StringSpec({

    // The provider map of a kontainer /////////////////////////////////////////////////////////////////////////////

    "getProvider of a class the blueprint does not know throws NoSuchElementException, with or without overrides" {

        val blueprint = kontainer {
            dynamic(PathsDyn::class)
        }

        listOf(
            blueprint.create(),
            blueprint.create { with { PathsDynOverride() } },
        ).forEach { kontainer ->
            shouldThrow<NoSuchElementException> {
                kontainer.getServiceProviderFactory().getProvider(PathsUnknown::class)
            }.message shouldBe "Key ${PathsUnknown::class} is missing in the map."
        }
    }

    "getAllProviders lists every service once in listing order, an override in the place of the service it replaces" {

        val blueprint = kontainer {
            singleton(PathsStatic::class)
            dynamic(PathsDyn::class)
            singleton(PathsSemi::class)
            prototype(PathsProto::class)
        }

        val plain = blueprint.create().getServiceProviderFactory().getAllProviders()
        val overridden = blueprint.create { with { PathsDynOverride() } }.getServiceProviderFactory().getAllProviders()

        assertSoftly {
            plain.keys.map { it.getName() } shouldBe listOf(
                "io.peekandpoke.ultra.kontainer.e2e.characterization.PathsStatic",
                "io.peekandpoke.ultra.kontainer.KontainerBlueprint",
                "io.peekandpoke.ultra.kontainer.e2e.characterization.PathsSemi",
                "io.peekandpoke.ultra.kontainer.e2e.characterization.PathsDyn",
                "io.peekandpoke.ultra.kontainer.Kontainer",
                "io.peekandpoke.ultra.kontainer.InjectionContext",
                "io.peekandpoke.ultra.kontainer.e2e.characterization.PathsProto",
            )

            overridden.keys.toList() shouldBe plain.keys.toList()

            plain.getValue(PathsDyn::class).reported shouldBe Reported.DynamicSingleton
            overridden.getValue(PathsDyn::class).reported shouldBe Reported.OverriddenDynamicSingleton
            overridden.getValue(PathsDyn::class).definition.producer.creates shouldBe PathsDynOverride::class

            overridden.filterKeys { it != PathsDyn::class }.mapValues { it.value.reported } shouldBe
                    plain.filterKeys { it != PathsDyn::class }.mapValues { it.value.reported }
        }
    }

    "getAllProviders creates the missing providers in the kontainer: getProvider hands out the same ones afterwards" {

        val kontainer = kontainer {
            singleton(PathsStatic::class)
            dynamic(PathsDyn::class)
            singleton(PathsSemi::class)
            prototype(PathsProto::class)
        }.create { with { PathsDynOverride() } }

        val factory = kontainer.getServiceProviderFactory()

        val before = factory.getProvider(PathsSemi::class)
        val all = factory.getAllProviders()

        assertSoftly {
            all.getValue(PathsSemi::class) shouldBeSameInstanceAs before

            all.forEach { (cls, provider) ->
                withClue(cls.getName()) {
                    factory.isProviderCreated(cls) shouldBe true
                    factory.getProvider(cls) shouldBeSameInstanceAs provider
                }
            }
        }
    }

    "An override is seen only by its own kontainer: kontainers without one, before and after, get the default" {

        val blueprint = kontainer {
            dynamic(PathsDyn::class)
            singleton(PathsSemi::class)
        }

        val before = blueprint.create()
        val withOverride = blueprint.create { with { PathsDynOverride() } }
        val after = blueprint.create()
        val clonedPlain = before.clone()

        assertSoftly {
            withOverride.get(PathsDyn::class).shouldBeInstanceOf<PathsDynOverride>()
            withOverride.get(PathsSemi::class).dyn.shouldBeInstanceOf<PathsDynOverride>()

            listOf(before, after, clonedPlain).forEach { kontainer ->
                kontainer.get(PathsDyn::class)::class shouldBe PathsDyn::class
                kontainer.get(PathsSemi::class).dyn::class shouldBe PathsDyn::class
                kontainer.getProvider(PathsDyn::class).reported shouldBe Reported.DynamicSingleton
            }
        }
    }

    // Resolution ///////////////////////////////////////////////////////////////////////////////////////////////////

    "get of an unknown class throws ServiceNotFound with its message, on every call" {

        val kontainer = kontainer { }.create()

        repeat(2) {
            shouldThrow<ServiceNotFound> {
                kontainer.get(PathsUnknown::class)
            }.message shouldBe "Service ${PathsUnknown::class.getName()} was not found"
        }
    }

    "get of a type with two candidates throws ServiceAmbiguous naming them, on every call" {

        val kontainer = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsOther::class)
        }.create()

        repeat(2) {
            shouldThrow<ServiceAmbiguous> {
                kontainer.get(PathsBase::class)
            }.message shouldBe "Service ${PathsBase::class.getName()} is ambiguous. It has multiple candidates: " +
                    "${PathsMiddle::class.getName()}, ${PathsOther::class.getName()}"
        }
    }

    "getOrNull returns the one candidate of a super type, and null for none or two, on every call" {

        val one = kontainer { singleton(PathsMiddle::class) }.create()
        val two = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsOther::class)
        }.create()

        repeat(2) {
            assertSoftly {
                one.getOrNull(PathsBase::class) shouldBeSameInstanceAs one.get(PathsMiddle::class)
                one.getOrNull(PathsUnknown::class) shouldBe null
                two.getOrNull(PathsBase::class) shouldBe null
            }
        }
    }

    "Aliased keys: getOrNull(Number) first, then Int and Integer share its entry, so Int finds the instance" {

        // Int::class (Java int) and the instance's class (java.lang.Integer) are equal keys of the candidate cache,
        // with different Java assignability: whichever form fills the shared entry first decides it
        val kontainer = kontainer { instance(5) }.create()

        assertSoftly {
            kontainer.getOrNull(Number::class) shouldBe 5
            kontainer.getOrNull(Int::class) shouldBe 5
            kontainer.get(Int::class) shouldBe 5
        }
    }

    "Aliased keys: getOrNull(Int) first leaves the shared entry empty, so getOrNull(Number) throws ServiceNotFound" {

        val kontainer = kontainer { instance(5) }.create()

        assertSoftly {
            kontainer.getOrNull(Int::class) shouldBe null
            shouldThrow<ServiceNotFound> {
                kontainer.getOrNull(Number::class)
            }.message shouldBe "Service kotlin.Int was not found"
        }
    }

    "getProvider of a super type is the provider of its one candidate" {

        val kontainer = kontainer { singleton(PathsMiddle::class) }.create()

        kontainer.getProvider(PathsBase::class) shouldBeSameInstanceAs kontainer.getProvider(PathsMiddle::class)
    }

    // Re-resolution of the candidates of getAll and Lookup (ULTRA-00351 finding 3) //////////////////////////////////

    "getAll of a base type whose candidates include a super type of another candidate throws ServiceAmbiguous" {

        // Today each candidate is resolved once more, and PathsMiddle has two candidates: itself and PathsLeaf
        val kontainer = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsLeaf::class)
        }.create()

        val message = "Service ${PathsMiddle::class.getName()} is ambiguous. It has multiple candidates: " +
                "${PathsMiddle::class.getName()}, ${PathsLeaf::class.getName()}"

        assertSoftly {
            shouldThrow<ServiceAmbiguous> { kontainer.getAll(PathsBase::class) }.message shouldBe message

            shouldThrow<ServiceAmbiguous> { kontainer.getAll(PathsMiddle::class) }.message shouldBe message

            kontainer.getAll(PathsLeaf::class) shouldBe listOf(kontainer.get(PathsLeaf::class))
        }
    }

    "List, Lazy List and Lookup injections of such a base type fail the same way, the Lookup only for the super type" {

        val kontainer = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsLeaf::class)
            singleton(PathsListHolder::class)
            singleton(PathsLazyListHolder::class)
            singleton(PathsLookupHolder::class)
        }.create()

        val message = "Service ${PathsMiddle::class.getName()} is ambiguous. It has multiple candidates: " +
                "${PathsMiddle::class.getName()}, ${PathsLeaf::class.getName()}"

        assertSoftly {
            shouldThrow<ServiceAmbiguous> { kontainer.get(PathsListHolder::class) }.message shouldBe message

            val lazyList = kontainer.get(PathsLazyListHolder::class).all
            shouldThrow<ServiceAmbiguous> { lazyList.value }.message shouldBe message

            val lookup = kontainer.get(PathsLookupHolder::class).all
            shouldThrow<ServiceAmbiguous> { lookup.get(PathsMiddle::class) }.message shouldBe message
            lookup.get(PathsLeaf::class) shouldBeSameInstanceAs kontainer.get(PathsLeaf::class)
        }
    }

    // How a class producer builds instances (ULTRA-00351 findings 6 to 8) ////////////////////////////////////////////

    "A no-arg class with a second all-optional constructor is accepted, and every get fails in createInstance" {

        val kontainer = kontainer { singleton(PathsTwoNoArg::class) }.create()

        repeat(2) {
            shouldThrow<IllegalArgumentException> {
                kontainer.get(PathsTwoNoArg::class)
            }.message shouldBe "Class should have a single no-arg constructor: ${PathsTwoNoArg::class}"
        }
    }

    "A throwing no-arg constructor fails get with what the reflection call throws, on every get" {

        val kontainer = kontainer { singleton(PathsThrowingNoArg::class) }.create()

        repeat(2) {
            val error = shouldThrow<InvocationTargetException> { kontainer.get(PathsThrowingNoArg::class) }

            error.cause.shouldBeInstanceOf<IllegalStateException>().message shouldBe "no-arg constructor failed"
        }
    }

    "A throwing constructor with arguments fails get with what the reflection call throws, on every get" {

        val kontainer = kontainer {
            singleton(PathsStatic::class)
            prototype(PathsThrowingWithArgs::class)
        }.create()

        repeat(2) {
            val error = shouldThrow<InvocationTargetException> { kontainer.get(PathsThrowingWithArgs::class) }

            error.cause.shouldBeInstanceOf<IllegalStateException>().message shouldBe "constructor with arguments failed"
        }
    }

    "A private no-arg constructor fails get with IllegalCallableAccessException" {

        val kontainer = kontainer { prototype(PathsPrivateNoArg::class) }.create()

        shouldThrow<IllegalCallableAccessException> { kontainer.get(PathsPrivateNoArg::class) }
    }

    "A private constructor with arguments fails get with IllegalCallableAccessException" {

        val kontainer = kontainer {
            singleton(PathsStatic::class)
            prototype(PathsPrivateWithArgs::class)
        }.create()

        shouldThrow<IllegalCallableAccessException> { kontainer.get(PathsPrivateWithArgs::class) }
    }

    "An object declared as a class service is refused at the definition: it has no primary constructor" {

        shouldThrow<InvalidClassProvided> {
            kontainer { singleton(PathsObject::class) }
        }.message shouldBe "Class '${PathsObject::class.qualifiedName}' has no primary constructor " +
                "and cannot be used as a service"
    }

    "A value class parameter is handed to the constructor unboxed, next to a service parameter" {

        val kontainer = kontainer {
            singleton(PathsId::class) { PathsId("id-1") }
            singleton(PathsStatic::class)
            prototype(PathsUsesId::class)
        }.create()

        val built = kontainer.get(PathsUsesId::class)

        assertSoftly {
            built.id shouldBe PathsId("id-1")
            built.static shouldBeSameInstanceAs kontainer.get(PathsStatic::class)
        }
    }
})
