package de.peekandpoke.ultra.playground

import de.peekandpoke.mutator.ListMutator
import de.peekandpoke.mutator.SetMutator
import de.peekandpoke.mutator.onChange
import de.peekandpoke.ultra.common.observe

object MutatorExamples {

    fun dataClassExample() {

        println("----------------------------------------------------------------------------------------")
        println("-- Simple Data Class example")

        val myInst = MyClass(name = "Hallo!")

        val mutator = myInst.mutator().onChange { println("Value changed: $it") }

        println("Is modified: ${mutator.isModified()}")
        mutator.name = "Hello World!"
        println("Is modified: ${mutator.isModified()}")

        val prop = mutator::name
        prop.set("Hello Prop!")

        mutator {
            name = "Hallo mutate()"
            address.city = "New Leipzig"
        }

        mutator {
            name = "Hallo mutate() 2"
            address {
                city = "New Leipzig 2"
                street = "New Street"
            }
        }

        val deepChange = mutator()

        println(
            "After deep change: $deepChange"
        )

        println()
    }

    fun listExample() {
        println("----------------------------------------------------------------------------------------")
        println("-- List Mutator Example")

        val value = listOf(
            MyClass("first", type = MyEnum.One),
            MyClass("second", type = MyEnum.One),
        )

        val mutator: ListMutator<MyClass> = value.mutator()
        observe(mutator) { it: List<MyClass> -> println("List changed: $it") }

        mutator.forEachIndexed { idx, it ->
            it.name = "Hello $idx!"
            it.type = MyEnum.Two
        }

        val pos2 = mutator[1]
        mutator.removeAt(0)
        pos2.name = "I am so alone"

        mutator.add(0, MyClass("Hello there!").mutator())

        pos2.name = "I am happy again!"

        println(mutator.get())

        println("---------------------------------------------------------------------------------------")

        mutator.modifyValue { it.map { e -> e.copy(name = "") }.toMutableList() }

        mutator[0].name = "I have a new name"

        println(mutator.get())

        println()
    }

    fun setExample() {
        println("----------------------------------------------------------------------------------------")
        println("-- Set Mutator Example")

        val input = setOf(
            MyClass("first", type = MyEnum.One),
            MyClass("second", type = MyEnum.One),
        )

        val mutator: SetMutator<MyClass> = input.mutator()
        observe(mutator) { it: Set<MyClass> -> println("Set changed: $it") }

        mutator.forEachIndexed { idx, it ->
            it.name = "Hello $idx!"
            it.type = MyEnum.Two
        }

        mutator.add(MyClass("Hello there!").mutator())

        println(mutator.get())

        println("---------------------------------------------------------------------------------------")

        mutator.modifyValue { it.mapIndexed { idx, e -> e.copy(name = "Idx $idx") }.toMutableSet() }

        mutator.first().name = "I have a new name"
//    println(mutator.value)

        println()
    }
}
