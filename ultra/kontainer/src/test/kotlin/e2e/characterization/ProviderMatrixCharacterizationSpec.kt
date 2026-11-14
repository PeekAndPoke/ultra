package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
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
                else -> error("unknown declaration $it")
            }
        }
        val types: List<Type> = types.split(" ").map { Type.valueOf(it) }
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
                classes.map { one.typeOf(it) } shouldBe row.types
            }

            withClue("types reported by kontainer two") {
                classes.map { two.typeOf(it) } shouldBe row.types
            }

            classes.zip(row.types).forEach { (cls, type) ->
                withClue("${cls.simpleName} is $type") {
                    when (type) {
                        Type.Singleton -> {
                            one.get(cls) shouldBeSameInstanceAs one.get(cls)
                            one.get(cls) shouldBeSameInstanceAs two.get(cls)
                        }

                        Type.SemiDynamic, Type.Dynamic -> {
                            one.get(cls) shouldBeSameInstanceAs one.get(cls)
                            two.get(cls) shouldBeSameInstanceAs two.get(cls)
                            one.get(cls) shouldNotBeSameInstanceAs two.get(cls)
                        }

                        Type.Prototype -> {
                            one.get(cls) shouldNotBeSameInstanceAs one.get(cls)
                        }

                        Type.DynamicOverride -> error("not used here")
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

                        when (row.types[idx]) {
                            Type.Prototype -> seen shouldNotBeSameInstanceAs k.get(childCls)
                            else -> seen shouldBeSameInstanceAs k.get(childCls)
                        }
                    }
                }
            }
        }
    }

    init {
        val direct = listOf(DirectA::class, DirectB::class)

        listOf(
            Row("S S", "Singleton Singleton", "1 1"),
            Row("S P", "Singleton Prototype", "1 1"),
            Row("S D", "SemiDynamic Dynamic", "2 2"),
            Row("P S", "Prototype Singleton", "4 1"),
            Row("P P", "Prototype Prototype", "4 4"),
            Row("P D", "Prototype Dynamic", "4 2"),
            Row("D S", "Dynamic Singleton", "2 1"),
            Row("D P", "Dynamic Prototype", "2 2"),
            Row("D D", "Dynamic Dynamic", "2 2"),
        ).forEach { row ->
            "Direct: ${row.name}" { run(direct, row) }
        }

        val two = listOf(TwoA::class, TwoB::class, TwoC::class)

        listOf(
            Row("S S S", "Singleton Singleton Singleton", "1 1 1"),
            Row("S S P", "Singleton Singleton Prototype", "1 1 1"),
            Row("S S D", "SemiDynamic SemiDynamic Dynamic", "2 2 2"),
            Row("S P S", "Singleton Prototype Singleton", "1 1 1"),
            Row("S P P", "Singleton Prototype Prototype", "1 1 1"),
            Row("S P D", "SemiDynamic Prototype Dynamic", "2 2 2"),
            Row("S D S", "SemiDynamic Dynamic Singleton", "2 2 1"),
            Row("S D P", "SemiDynamic Dynamic Prototype", "2 2 2"),
            Row("S D D", "SemiDynamic Dynamic Dynamic", "2 2 2"),
            Row("P S S", "Prototype Singleton Singleton", "4 1 1"),
            Row("P S P", "Prototype Singleton Prototype", "4 1 1"),
            Row("P S D", "Prototype SemiDynamic Dynamic", "4 2 2"),
            Row("P P S", "Prototype Prototype Singleton", "4 4 1"),
            Row("P P P", "Prototype Prototype Prototype", "4 4 4"),
            Row("P P D", "Prototype Prototype Dynamic", "4 4 2"),
            Row("P D S", "Prototype Dynamic Singleton", "4 2 1"),
            Row("P D P", "Prototype Dynamic Prototype", "4 2 2"),
            Row("P D D", "Prototype Dynamic Dynamic", "4 2 2"),
            Row("D S S", "Dynamic Singleton Singleton", "2 1 1"),
            Row("D S P", "Dynamic Singleton Prototype", "2 1 1"),
            Row("D S D", "Dynamic SemiDynamic Dynamic", "2 2 2"),
            Row("D P S", "Dynamic Prototype Singleton", "2 2 1"),
            Row("D P P", "Dynamic Prototype Prototype", "2 2 2"),
            Row("D P D", "Dynamic Prototype Dynamic", "2 2 2"),
            Row("D D S", "Dynamic Dynamic Singleton", "2 2 1"),
            Row("D D P", "Dynamic Dynamic Prototype", "2 2 2"),
            Row("D D D", "Dynamic Dynamic Dynamic", "2 2 2"),
        ).forEach { row ->
            "Two levels: ${row.name}" { run(two, row) }
        }

        val three = listOf(ThreeA::class, ThreeB::class, ThreeC::class, ThreeD::class)

        listOf(
            Row("S S S D", "SemiDynamic SemiDynamic SemiDynamic Dynamic", "2 2 2 2"),
            Row("S P P D", "SemiDynamic Prototype Prototype Dynamic", "2 2 2 2"),
            Row("S P S D", "SemiDynamic Prototype SemiDynamic Dynamic", "2 2 2 2"),
            Row("P S S D", "Prototype SemiDynamic SemiDynamic Dynamic", "4 2 2 2"),
            Row("S S P S", "Singleton Singleton Prototype Singleton", "1 1 1 1"),
            Row("D S S S", "Dynamic Singleton Singleton Singleton", "2 1 1 1"),
            Row("S D P S", "SemiDynamic Dynamic Prototype Singleton", "2 2 2 1"),
            Row("P P P D", "Prototype Prototype Prototype Dynamic", "4 4 4 2"),
            Row("D S P D", "Dynamic SemiDynamic Prototype Dynamic", "2 2 2 2"),
            Row("S P D S", "SemiDynamic Prototype Dynamic Singleton", "2 2 2 1"),
        ).forEach { row ->
            "Three levels: ${row.name}" { run(three, row) }
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
                one.typeOf(TwoA::class) shouldBe Type.SemiDynamic
                one.typeOf(TwoB::class) shouldBe Type.Prototype
                one.typeOf(TwoC::class) shouldBe Type.Dynamic

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
