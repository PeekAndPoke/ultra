package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

class OrderDyn
class OrderMissing
class OrderSemi(val dyn: OrderDyn)
class OrderProtoAbove(val dyn: OrderDyn)
class OrderProtoPlain
class OrderProtoAboveBroken(val dyn: OrderDyn, val missing: OrderMissing)
class OrderProtoPlainBroken(val missing: OrderMissing)

class OrderLow(val dyn: OrderDyn)
class OrderHigh(val low: OrderLow)
class OrderTop(val high: OrderHigh)
class OrderLowBroken(val dyn: OrderDyn, val missing: OrderMissing)
class OrderHighBroken(val low: OrderLowBroken, val missing: OrderMissing)
class OrderTopBroken(val high: OrderHighBroken)

class OrderSemiOuter(val inner: OrderSemiInner)
class OrderSemiInner(val dyn: OrderDyn)
class OrderSemiOuterBroken(val inner: OrderSemiInnerBroken, val missing: OrderMissing)
class OrderSemiInnerBroken(val dyn: OrderDyn, val missing: OrderMissing)

/**
 * Pins the order in which services are listed and validation errors are numbered: prototypes in definition order,
 * whatever they reach; semi dynamic singletons in discovery order. Uses only API that exists before and after the
 * type and kind split, so it was run against both.
 */
class ListingOrderCharacterizationSpec : StringSpec({

    fun Kontainer.listed(vararg classes: KClass<*>): List<String> {
        val names = classes.map { it.java.name }.toSet()
        return tools.getDebugInfo().services.map { it.cls.fqn }.filter { it in names }
    }

    fun KontainerInconsistent.numbered(first: KClass<*>, second: KClass<*>) {
        val text = message!!
        text shouldContain "1. Service '${first.qualifiedName}'"
        text shouldContain "2. Service '${second.qualifiedName}'"
        text.indexOf(first.qualifiedName!!) shouldBeLessThan text.indexOf(second.qualifiedName!!)
    }

    "A prototype above a dynamic is listed in definition order, before a later plain prototype" {

        val subject = kontainer {
            dynamic(OrderDyn::class)
            singleton(OrderSemi::class)
            prototype(OrderProtoAbove::class)
            prototype(OrderProtoPlain::class)
        }.create()

        assertSoftly {
            subject.listed(OrderProtoAbove::class, OrderProtoPlain::class) shouldBe
                    listOf(OrderProtoAbove::class.java.name, OrderProtoPlain::class.java.name)
        }
    }

    "Validation numbers a prototype above a dynamic in definition order, before a later plain prototype" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                dynamic(OrderDyn::class)
                singleton(OrderSemi::class)
                prototype(OrderProtoAboveBroken::class)
                prototype(OrderProtoPlainBroken::class)
            }.create()
        }

        error.numbered(OrderProtoAboveBroken::class, OrderProtoPlainBroken::class)
    }

    "Two prototypes in a chain above a dynamic are listed in definition order, not discovery order" {

        val subject = kontainer {
            singleton(OrderTop::class)
            prototype(OrderHigh::class)
            prototype(OrderLow::class)
            dynamic(OrderDyn::class)
        }.create()

        assertSoftly {
            subject.listed(OrderHigh::class, OrderLow::class) shouldBe
                    listOf(OrderHigh::class.java.name, OrderLow::class.java.name)
        }
    }

    "Validation numbers two prototypes in a chain above a dynamic in definition order" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(OrderTopBroken::class)
                prototype(OrderHighBroken::class)
                prototype(OrderLowBroken::class)
                dynamic(OrderDyn::class)
            }.create()
        }

        error.numbered(OrderHighBroken::class, OrderLowBroken::class)
    }

    "Semi dynamic singletons are listed in discovery order: the one injecting the dynamic first" {

        val subject = kontainer {
            singleton(OrderSemiOuter::class)
            singleton(OrderSemiInner::class)
            dynamic(OrderDyn::class)
        }.create()

        subject.listed(OrderSemiOuter::class, OrderSemiInner::class) shouldBe
                listOf(OrderSemiInner::class.java.name, OrderSemiOuter::class.java.name)
    }

    "Validation numbers semi dynamic singletons in discovery order" {

        val error = shouldThrow<KontainerInconsistent> {
            kontainer {
                singleton(OrderSemiOuterBroken::class)
                singleton(OrderSemiInnerBroken::class)
                dynamic(OrderDyn::class)
            }.create()
        }

        error.numbered(OrderSemiInnerBroken::class, OrderSemiOuterBroken::class)
    }
})
