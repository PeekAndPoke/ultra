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
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
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
class PathsNumbersHolder(val all: List<Number>)

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
 * Pins the provision paths ULTRA-00351 makes faster: the provider map of a kontainer, resolution, getAll, List and
 * Lookup (each candidate's own service since ULTRA-00352), and how a class producer builds its instances.
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

    "A value type cannot be registered: instance(5) is refused, so Int and Integer never share a key (ULTRA-00354)" {

        // Before, Int::class (Java int) and the instance's class (java.lang.Integer) were equal keys of the candidate
        // cache with different Java assignability: whichever form filled the shared entry first decided the answers
        shouldThrow<KontainerInconsistent> {
            kontainer { instance(5) }
        }.message shouldBe "kotlin.Int is a value type and cannot be registered"
    }

    "A Number service: getOrNull(Number) finds it and getOrNull(Int) finds nothing, in either order" {

        val numberFirst = kontainer { instance(Number::class, 5) }.create()
        val intFirst = kontainer { instance(Number::class, 5) }.create()

        assertSoftly {
            numberFirst.getOrNull(Number::class) shouldBe 5
            numberFirst.getOrNull(Int::class) shouldBe null

            intFirst.getOrNull(Int::class) shouldBe null
            intFirst.getOrNull(Number::class) shouldBe 5
        }
    }

    "getProvider of a super type is the provider of its one candidate" {

        val kontainer = kontainer { singleton(PathsMiddle::class) }.create()

        kontainer.getProvider(PathsBase::class) shouldBeSameInstanceAs kontainer.getProvider(PathsMiddle::class)
    }

    // getAll, List and Lookup hand out each candidate's own service (ULTRA-00352) ////////////////////////////////////

    "getAll returns each candidate's own service in candidate order, also when one candidate extends another" {

        // PathsMiddle is a super type of PathsLeaf: resolving it once more would be ambiguous
        val kontainer = kontainer {
            singleton(PathsLeaf::class)
            singleton(PathsMiddle::class)
        }.create()

        val leaf = kontainer.get(PathsLeaf::class)
        val all = kontainer.getAll(PathsBase::class)

        assertSoftly {
            kontainer.getCandidates(PathsBase::class).toList() shouldBe listOf(PathsLeaf::class, PathsMiddle::class)
            all.map { it::class } shouldBe listOf(PathsLeaf::class, PathsMiddle::class)
            all[0] shouldBeSameInstanceAs leaf

            // Singletons: every form hands out the same instances
            kontainer.getAll(PathsBase::class).zip(all).forEach { (a, b) -> a shouldBeSameInstanceAs b }
            kontainer.getAll(PathsMiddle::class).map { it::class } shouldBe listOf(PathsLeaf::class, PathsMiddle::class)
            kontainer.getAll(PathsMiddle::class)[1] shouldBeSameInstanceAs all[1]
            kontainer.getAll(PathsLeaf::class) shouldBe listOf(leaf)
        }
    }

    "get of such a super type still throws ServiceAmbiguous naming both candidates" {

        val kontainer = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsLeaf::class)
        }.create()

        val message = "Service ${PathsMiddle::class.getName()} is ambiguous. It has multiple candidates: " +
                "${PathsMiddle::class.getName()}, ${PathsLeaf::class.getName()}"

        assertSoftly {
            shouldThrow<ServiceAmbiguous> { kontainer.get(PathsMiddle::class) }.message shouldBe message
            shouldThrow<ServiceAmbiguous> { kontainer.getProvider(PathsMiddle::class) }.message shouldBe message
            kontainer.getOrNull(PathsMiddle::class) shouldBe null
        }
    }

    "List, Lazy List and Lookup injections of such a base type hand out each candidate's own service" {

        val kontainer = kontainer {
            singleton(PathsMiddle::class)
            singleton(PathsLeaf::class)
            singleton(PathsListHolder::class)
            singleton(PathsLazyListHolder::class)
            singleton(PathsLookupHolder::class)
        }.create()

        val all = kontainer.getAll(PathsBase::class)

        assertSoftly {
            all.map { it::class } shouldBe listOf(PathsMiddle::class, PathsLeaf::class)

            val list = kontainer.get(PathsListHolder::class).all
            list.size shouldBe 2
            list.zip(all).forEach { (a, b) -> a shouldBeSameInstanceAs b }

            val lazyList = kontainer.get(PathsLazyListHolder::class).all.value
            lazyList.size shouldBe 2
            lazyList.zip(all).forEach { (a, b) -> a shouldBeSameInstanceAs b }

            val lookup = kontainer.get(PathsLookupHolder::class).all
            lookup.get(PathsMiddle::class) shouldBeSameInstanceAs all[0]
            lookup.get(PathsLeaf::class) shouldBeSameInstanceAs all[1]
        }
    }

    "getAll and the List injection hand out each candidate as get would: the override, a new prototype each time" {

        val kontainer = kontainer {
            dynamic(PathsMiddle::class)
            prototype(PathsOther::class)
            singleton(PathsListHolder::class)
        }.create {
            with(PathsMiddle::class) { PathsMiddle() }
        }

        val overridden = kontainer.get(PathsMiddle::class)
        val first = kontainer.getAll(PathsBase::class)
        val second = kontainer.getAll(PathsBase::class)
        val list = kontainer.get(PathsListHolder::class).all

        assertSoftly {
            kontainer.getProvider(PathsMiddle::class).reported shouldBe Reported.OverriddenDynamicSingleton

            first.map { it::class } shouldBe listOf(PathsMiddle::class, PathsOther::class)
            first[0] shouldBeSameInstanceAs overridden
            second[0] shouldBeSameInstanceAs overridden
            list[0] shouldBeSameInstanceAs overridden

            (first[1] === second[1]) shouldBe false
            (list[1] === first[1]) shouldBe false
        }
    }

    "A Number service: getAll, a Lookup and a List injection of Number find it, and Int still finds nothing" {

        val kontainer = kontainer {
            instance(Number::class, 5)
            singleton(PathsNumbersHolder::class)
        }.create()

        assertSoftly {
            kontainer.getAll(Number::class) shouldBe listOf(5)
            kontainer.getLookup(Number::class).get(Number::class) shouldBe 5
            kontainer.get(PathsNumbersHolder::class).all shouldBe listOf(5)

            kontainer.getOrNull(Int::class) shouldBe null
            kontainer.getAll(Int::class) shouldBe emptyList()
            runCatching { kontainer.get(Int::class) }.exceptionOrNull()?.message shouldBe
                    "Service kotlin.Int was not found"
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
