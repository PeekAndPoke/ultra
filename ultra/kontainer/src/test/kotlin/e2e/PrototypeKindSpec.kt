package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.e2e.characterization.Births
import io.peekandpoke.ultra.kontainer.e2e.characterization.Counted
import io.peekandpoke.ultra.kontainer.kontainer

/**
 * Pins the kind of a plain prototype: SemiDynamic when it reaches anything dynamic, Static otherwise.
 */
class PrototypeKindSpec : StringSpec({

    "A prototype that injects nothing dynamic is Prototype.Static" {

        class Single
        class Proto(val single: Single)

        val subject = kontainer {
            singleton(Single::class)
            prototype(Proto::class)
        }.create()

        subject.reportedOf(Proto::class) shouldBe Reported.StaticPrototype
    }

    "A prototype that injects a dynamic is Prototype.SemiDynamic, and is still new on every get" {

        class Dyn
        class Proto(val dyn: Dyn) : Counted()

        Births.reset()

        val subject = kontainer {
            dynamic(Dyn::class)
            prototype(Proto::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Proto::class) shouldBe Reported.SemiDynamicPrototype
            subject.get(Proto::class) shouldNotBeSameInstanceAs subject.get(Proto::class)
            subject.get(Proto::class).dyn shouldBeSameInstanceAs subject.get(Dyn::class)
            Births.of(Proto::class) shouldBe 3
        }
    }

    "A prototype that reaches a dynamic through a singleton is Prototype.SemiDynamic" {

        class Dyn
        class Promoted(val dyn: Dyn)
        class Proto(val promoted: Promoted)

        val subject = kontainer {
            dynamic(Dyn::class)
            singleton(Promoted::class)
            prototype(Proto::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Promoted::class) shouldBe Reported.SemiDynamicSingleton
            subject.reportedOf(Proto::class) shouldBe Reported.SemiDynamicPrototype
        }
    }

    "A prototype that reaches a dynamic through another prototype is Prototype.SemiDynamic, both of them" {

        class Dyn
        class Low(val dyn: Dyn)
        class High(val low: Low)

        val subject = kontainer {
            dynamic(Dyn::class)
            prototype(Low::class)
            prototype(High::class)
        }.create()

        assertSoftly {
            subject.reportedOf(Low::class) shouldBe Reported.SemiDynamicPrototype
            subject.reportedOf(High::class) shouldBe Reported.SemiDynamicPrototype
        }
    }

    "A prototype that injects a dynamicPrototype is Prototype.SemiDynamic" {

        class DynProto
        class Proto(val dynProto: DynProto)

        val subject = kontainer {
            dynamicPrototype(DynProto::class)
            prototype(Proto::class)
        }.create()

        subject.reportedOf(Proto::class) shouldBe Reported.SemiDynamicPrototype
    }

    "A prototype is Prototype.SemiDynamic through Lazy, List, Lookup, InjectionContext and Kontainer" {

        abstract class Base
        class DynImpl : Base()
        class ViaLazy(val dyn: Lazy<DynImpl>)
        class ViaList(val all: List<Base>)
        class ViaLookup(val lookup: Lookup<Base>)
        class ViaContext(val context: InjectionContext)
        class ViaKontainer(val kontainer: Kontainer)

        val subject = kontainer {
            dynamic(DynImpl::class)
            prototype(ViaLazy::class)
            prototype(ViaList::class)
            prototype(ViaLookup::class)
            prototype(ViaContext::class)
            prototype(ViaKontainer::class)
        }.create()

        assertSoftly {
            subject.reportedOf(ViaLazy::class) shouldBe Reported.SemiDynamicPrototype
            subject.reportedOf(ViaList::class) shouldBe Reported.SemiDynamicPrototype
            subject.reportedOf(ViaLookup::class) shouldBe Reported.SemiDynamicPrototype
            subject.reportedOf(ViaContext::class) shouldBe Reported.SemiDynamicPrototype
            subject.reportedOf(ViaKontainer::class) shouldBe Reported.SemiDynamicPrototype
        }
    }

    "A dynamic that injects a dynamic stays Singleton.Dynamic, a dynamicPrototype stays Prototype.Dynamic" {

        class Dyn
        class DynAbove(val dyn: Dyn)
        class DynProtoAbove(val dyn: Dyn)

        val subject = kontainer {
            dynamic(Dyn::class)
            dynamic(DynAbove::class)
            dynamicPrototype(DynProtoAbove::class)
        }.create()

        assertSoftly {
            subject.reportedOf(DynAbove::class) shouldBe Reported.DynamicSingleton
            subject.reportedOf(DynProtoAbove::class) shouldBe Reported.DynamicPrototype
        }
    }
})
