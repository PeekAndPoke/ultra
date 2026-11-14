package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reportedOf
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

/** A service in a chain; [next] is the service it injects. */
abstract class ChainNode : Counted() {
    abstract val next: ChainNode?
}

class DirectA(override val next: DirectB) : ChainNode()
class DirectB : ChainNode() {
    override val next: ChainNode? get() = null
}

class TwoA(override val next: TwoB) : ChainNode()
class TwoB(override val next: TwoC) : ChainNode()
class TwoC : ChainNode() {
    override val next: ChainNode? get() = null
}

class ThreeA(override val next: ThreeB) : ChainNode()
class ThreeB(override val next: ThreeC) : ChainNode()
class ThreeC(override val next: ThreeD) : ChainNode()
class ThreeD : ChainNode() {
    override val next: ChainNode? get() = null
}

/**
 * Pins provider x dependency for chains of two, three and four services: the reported type, the constructor runs
 * and which instances are shared.
 *
 * The scenario of every row: kontainer one gets the head twice, then kontainer two gets the head twice. The births
 * are counted after that; the identity checks come after the count.
 */
class ProviderMatrixCharacterizationSpec : StringSpec() {

    private class Row(declared: String, types: String, births: String) {
        val declared: List<Declared> = declared.split(" ").map {
            when (it) {
                "S" -> Declared.Singleton
                "P" -> Declared.Prototype
                "D" -> Declared.Dynamic
                "DP" -> Declared.DynamicPrototype
                else -> error("unknown declaration $it")
            }
        }
        val types: List<Reported> = types.split(" ").map { Reported.parse(it) }
        val births: List<Int> = births.split(" ").map { it.toInt() }

        val name = this.declared.joinToString(" -> ") + " gives " + this.types.joinToString(", ") +
                "; births " + this.births.joinToString(", ")
    }

    private fun run(classes: List<KClass<out ChainNode>>, row: Row) {

        Births.reset()

        val blueprint = kontainer {
            classes.zip(row.declared).forEach { (cls, declared) ->
                @Suppress("UNCHECKED_CAST")
                declare(declared)(cls as KClass<Any>)
            }
        }

        val one = blueprint.create()
        repeat(2) { one.get(classes[0]) }
        val two = blueprint.create()
        repeat(2) { two.get(classes[0]) }

        val births = classes.map { Births.of(it) }

        assertSoftly {
            withClue("constructor runs") {
                births shouldBe row.births
            }

            withClue("types reported by kontainer one") {
                classes.map { one.reportedOf(it) } shouldBe row.types
            }

            withClue("types reported by kontainer two") {
                classes.map { two.reportedOf(it) } shouldBe row.types
            }

            classes.zip(row.types).forEach { (cls, reported) ->
                withClue("${cls.simpleName} is $reported") {
                    when {
                        reported.type == ServiceType.Prototype -> {
                            one.get(cls) shouldNotBeSameInstanceAs one.get(cls)
                        }

                        reported.kind == ServiceKind.Static -> {
                            one.get(cls) shouldBeSameInstanceAs one.get(cls)
                            one.get(cls) shouldBeSameInstanceAs two.get(cls)
                        }

                        else -> {
                            one.get(cls) shouldBeSameInstanceAs one.get(cls)
                            two.get(cls) shouldBeSameInstanceAs two.get(cls)
                            one.get(cls) shouldNotBeSameInstanceAs two.get(cls)
                        }
                    }
                }
            }

            listOf("one" to one, "two" to two).forEach { (name, k) ->
                classes.indices.drop(1).forEach { idx ->
                    val parentCls = classes[idx - 1]
                    val childCls = classes[idx]

                    withClue("kontainer $name: what ${parentCls.simpleName} sees of ${childCls.simpleName}") {
                        val seen = k.get(parentCls).next!!

                        seen::class shouldBe childCls

                        when (row.types[idx].type) {
                            ServiceType.Prototype -> seen shouldNotBeSameInstanceAs k.get(childCls)
                            ServiceType.Singleton -> seen shouldBeSameInstanceAs k.get(childCls)
                        }
                    }
                }
            }
        }
    }

    init {
        val direct = listOf(DirectA::class, DirectB::class)

        listOf(
            Row("S S", "Singleton.Static Singleton.Static", "1 1"),
            Row("S P", "Singleton.Static Prototype.Static", "1 1"),
            Row("S D", "Singleton.SemiDynamic Singleton.Dynamic", "2 2"),
            Row("P S", "Prototype.Static Singleton.Static", "4 1"),
            Row("P P", "Prototype.Static Prototype.Static", "4 4"),
            Row("P D", "Prototype.SemiDynamic Singleton.Dynamic", "4 2"),
            Row("D S", "Singleton.Dynamic Singleton.Static", "2 1"),
            Row("D P", "Singleton.Dynamic Prototype.Static", "2 2"),
            Row("D D", "Singleton.Dynamic Singleton.Dynamic", "2 2"),
        ).forEach { row ->
            "Direct: ${row.name}" { run(direct, row) }
        }

        val two = listOf(TwoA::class, TwoB::class, TwoC::class)

        listOf(
            Row("S S S", "Singleton.Static Singleton.Static Singleton.Static", "1 1 1"),
            Row("S S P", "Singleton.Static Singleton.Static Prototype.Static", "1 1 1"),
            Row("S S D", "Singleton.SemiDynamic Singleton.SemiDynamic Singleton.Dynamic", "2 2 2"),
            Row("S P S", "Singleton.Static Prototype.Static Singleton.Static", "1 1 1"),
            Row("S P P", "Singleton.Static Prototype.Static Prototype.Static", "1 1 1"),
            Row("S P D", "Singleton.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic", "2 2 2"),
            Row("S D S", "Singleton.SemiDynamic Singleton.Dynamic Singleton.Static", "2 2 1"),
            Row("S D P", "Singleton.SemiDynamic Singleton.Dynamic Prototype.Static", "2 2 2"),
            Row("S D D", "Singleton.SemiDynamic Singleton.Dynamic Singleton.Dynamic", "2 2 2"),
            Row("P S S", "Prototype.Static Singleton.Static Singleton.Static", "4 1 1"),
            Row("P S P", "Prototype.Static Singleton.Static Prototype.Static", "4 1 1"),
            Row("P S D", "Prototype.SemiDynamic Singleton.SemiDynamic Singleton.Dynamic", "4 2 2"),
            Row("P P S", "Prototype.Static Prototype.Static Singleton.Static", "4 4 1"),
            Row("P P P", "Prototype.Static Prototype.Static Prototype.Static", "4 4 4"),
            Row("P P D", "Prototype.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic", "4 4 2"),
            Row("P D S", "Prototype.SemiDynamic Singleton.Dynamic Singleton.Static", "4 2 1"),
            Row("P D P", "Prototype.SemiDynamic Singleton.Dynamic Prototype.Static", "4 2 2"),
            Row("P D D", "Prototype.SemiDynamic Singleton.Dynamic Singleton.Dynamic", "4 2 2"),
            Row("D S S", "Singleton.Dynamic Singleton.Static Singleton.Static", "2 1 1"),
            Row("D S P", "Singleton.Dynamic Singleton.Static Prototype.Static", "2 1 1"),
            Row("D S D", "Singleton.Dynamic Singleton.SemiDynamic Singleton.Dynamic", "2 2 2"),
            Row("D P S", "Singleton.Dynamic Prototype.Static Singleton.Static", "2 2 1"),
            Row("D P P", "Singleton.Dynamic Prototype.Static Prototype.Static", "2 2 2"),
            Row("D P D", "Singleton.Dynamic Prototype.SemiDynamic Singleton.Dynamic", "2 2 2"),
            Row("D D S", "Singleton.Dynamic Singleton.Dynamic Singleton.Static", "2 2 1"),
            Row("D D P", "Singleton.Dynamic Singleton.Dynamic Prototype.Static", "2 2 2"),
            Row("D D D", "Singleton.Dynamic Singleton.Dynamic Singleton.Dynamic", "2 2 2"),
        ).forEach { row ->
            "Two levels: ${row.name}" { run(two, row) }
        }

        val three = listOf(ThreeA::class, ThreeB::class, ThreeC::class, ThreeD::class)

        listOf(
            Row("S S S D", "Singleton.SemiDynamic Singleton.SemiDynamic Singleton.SemiDynamic Singleton.Dynamic", "2 2 2 2"),
            Row("S P P D", "Singleton.SemiDynamic Prototype.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic", "2 2 2 2"),
            Row("S P S D", "Singleton.SemiDynamic Prototype.SemiDynamic Singleton.SemiDynamic Singleton.Dynamic", "2 2 2 2"),
            Row("P S S D", "Prototype.SemiDynamic Singleton.SemiDynamic Singleton.SemiDynamic Singleton.Dynamic", "4 2 2 2"),
            Row("S S P S", "Singleton.Static Singleton.Static Prototype.Static Singleton.Static", "1 1 1 1"),
            Row("D S S S", "Singleton.Dynamic Singleton.Static Singleton.Static Singleton.Static", "2 1 1 1"),
            Row("S D P S", "Singleton.SemiDynamic Singleton.Dynamic Prototype.Static Singleton.Static", "2 2 2 1"),
            Row("P P P D", "Prototype.SemiDynamic Prototype.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic", "4 4 4 2"),
            Row("D S P D", "Singleton.Dynamic Singleton.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic", "2 2 2 2"),
            Row("S P D S", "Singleton.SemiDynamic Prototype.SemiDynamic Singleton.Dynamic Singleton.Static", "2 2 2 1"),
        ).forEach { row ->
            "Three levels: ${row.name}" { run(three, row) }
        }

        listOf(
            Row("S DP", "Singleton.SemiDynamic Prototype.Dynamic", "2 2"),
            Row("P DP", "Prototype.SemiDynamic Prototype.Dynamic", "4 4"),
            Row("D DP", "Singleton.Dynamic Prototype.Dynamic", "2 2"),
            Row("DP S", "Prototype.Dynamic Singleton.Static", "4 1"),
            Row("DP P", "Prototype.Dynamic Prototype.Static", "4 4"),
            Row("DP D", "Prototype.Dynamic Singleton.Dynamic", "4 2"),
            Row("DP DP", "Prototype.Dynamic Prototype.Dynamic", "4 4"),
        ).forEach { row ->
            "Direct, dynamicPrototype: ${row.name}" { run(direct, row) }
        }

        listOf(
            Row("S S DP", "Singleton.SemiDynamic Singleton.SemiDynamic Prototype.Dynamic", "2 2 2"),
            Row("S P DP", "Singleton.SemiDynamic Prototype.SemiDynamic Prototype.Dynamic", "2 2 2"),
            Row("S D DP", "Singleton.SemiDynamic Singleton.Dynamic Prototype.Dynamic", "2 2 2"),
            Row("S DP S", "Singleton.SemiDynamic Prototype.Dynamic Singleton.Static", "2 2 1"),
            Row("S DP P", "Singleton.SemiDynamic Prototype.Dynamic Prototype.Static", "2 2 2"),
            Row("S DP D", "Singleton.SemiDynamic Prototype.Dynamic Singleton.Dynamic", "2 2 2"),
            Row("S DP DP", "Singleton.SemiDynamic Prototype.Dynamic Prototype.Dynamic", "2 2 2"),
            Row("P S DP", "Prototype.SemiDynamic Singleton.SemiDynamic Prototype.Dynamic", "4 2 2"),
            Row("P P DP", "Prototype.SemiDynamic Prototype.SemiDynamic Prototype.Dynamic", "4 4 4"),
            Row("P D DP", "Prototype.SemiDynamic Singleton.Dynamic Prototype.Dynamic", "4 2 2"),
            Row("P DP S", "Prototype.SemiDynamic Prototype.Dynamic Singleton.Static", "4 4 1"),
            Row("P DP P", "Prototype.SemiDynamic Prototype.Dynamic Prototype.Static", "4 4 4"),
            Row("P DP D", "Prototype.SemiDynamic Prototype.Dynamic Singleton.Dynamic", "4 4 2"),
            Row("P DP DP", "Prototype.SemiDynamic Prototype.Dynamic Prototype.Dynamic", "4 4 4"),
            Row("D S DP", "Singleton.Dynamic Singleton.SemiDynamic Prototype.Dynamic", "2 2 2"),
            Row("D P DP", "Singleton.Dynamic Prototype.SemiDynamic Prototype.Dynamic", "2 2 2"),
            Row("D D DP", "Singleton.Dynamic Singleton.Dynamic Prototype.Dynamic", "2 2 2"),
            Row("D DP S", "Singleton.Dynamic Prototype.Dynamic Singleton.Static", "2 2 1"),
            Row("D DP P", "Singleton.Dynamic Prototype.Dynamic Prototype.Static", "2 2 2"),
            Row("D DP D", "Singleton.Dynamic Prototype.Dynamic Singleton.Dynamic", "2 2 2"),
            Row("D DP DP", "Singleton.Dynamic Prototype.Dynamic Prototype.Dynamic", "2 2 2"),
            Row("DP S S", "Prototype.Dynamic Singleton.Static Singleton.Static", "4 1 1"),
            Row("DP S P", "Prototype.Dynamic Singleton.Static Prototype.Static", "4 1 1"),
            Row("DP S D", "Prototype.Dynamic Singleton.SemiDynamic Singleton.Dynamic", "4 2 2"),
            Row("DP S DP", "Prototype.Dynamic Singleton.SemiDynamic Prototype.Dynamic", "4 2 2"),
            Row("DP P S", "Prototype.Dynamic Prototype.Static Singleton.Static", "4 4 1"),
            Row("DP P P", "Prototype.Dynamic Prototype.Static Prototype.Static", "4 4 4"),
            Row("DP P D", "Prototype.Dynamic Prototype.SemiDynamic Singleton.Dynamic", "4 4 2"),
            Row("DP P DP", "Prototype.Dynamic Prototype.SemiDynamic Prototype.Dynamic", "4 4 4"),
            Row("DP D S", "Prototype.Dynamic Singleton.Dynamic Singleton.Static", "4 2 1"),
            Row("DP D P", "Prototype.Dynamic Singleton.Dynamic Prototype.Static", "4 2 2"),
            Row("DP D D", "Prototype.Dynamic Singleton.Dynamic Singleton.Dynamic", "4 2 2"),
            Row("DP D DP", "Prototype.Dynamic Singleton.Dynamic Prototype.Dynamic", "4 2 2"),
            Row("DP DP S", "Prototype.Dynamic Prototype.Dynamic Singleton.Static", "4 4 1"),
            Row("DP DP P", "Prototype.Dynamic Prototype.Dynamic Prototype.Static", "4 4 4"),
            Row("DP DP D", "Prototype.Dynamic Prototype.Dynamic Singleton.Dynamic", "4 4 2"),
            Row("DP DP DP", "Prototype.Dynamic Prototype.Dynamic Prototype.Dynamic", "4 4 4"),
        ).forEach { row ->
            "Two levels, dynamicPrototype: ${row.name}" { run(two, row) }
        }

        "Singleton -> Prototype -> Dynamic: the singleton becomes SemiDynamic, the prototype stays Prototype" {

            Births.reset()

            val blueprint = kontainer {
                singleton(TwoA::class)
                prototype(TwoB::class)
                dynamic(TwoC::class)
            }

            val one = blueprint.create()
            val two = blueprint.create()

            val headOne = one.get(TwoA::class)
            val headTwo = two.get(TwoA::class)

            assertSoftly {
                one.reportedOf(TwoA::class) shouldBe Reported.SemiDynamicSingleton
                one.reportedOf(TwoB::class) shouldBe Reported.SemiDynamicPrototype
                one.reportedOf(TwoC::class) shouldBe Reported.DynamicSingleton

                withClue("one singleton, one prototype and one dynamic per kontainer") {
                    Births.of(TwoA::class) shouldBe 2
                    Births.of(TwoB::class) shouldBe 2
                    Births.of(TwoC::class) shouldBe 2
                }

                withClue("each singleton sees the dynamic of its own kontainer through the prototype") {
                    headOne.next.next shouldBeSameInstanceAs one.get(TwoC::class)
                    headTwo.next.next shouldBeSameInstanceAs two.get(TwoC::class)
                    headOne.next.next shouldNotBeSameInstanceAs headTwo.next.next
                }

                withClue("the singleton is kept per kontainer") {
                    one.get(TwoA::class) shouldBeSameInstanceAs headOne
                    two.get(TwoA::class) shouldBeSameInstanceAs headTwo
                }
            }
        }
    }
}
