package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.observe
import de.peekandpoke.ultra.playground.lib.DataClassMutator
import de.peekandpoke.ultra.playground.lib.ListMutator
import de.peekandpoke.ultra.playground.lib.Mutation
import de.peekandpoke.ultra.playground.lib.Mutator
import de.peekandpoke.ultra.playground.lib.SetMutator
import de.peekandpoke.ultra.playground.lib.add
import de.peekandpoke.ultra.playground.lib.mutator

////  USER CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

enum class MyEnum {
    One,
    Two,
}

data class MyClass(val name: String, val type: MyEnum = MyEnum.One)

////  GENERATED CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

class MyClassMutator(value: MyClass) : DataClassMutator<MyClass>(value)

fun MyClass.mutator(): MyClassMutator =
    MyClassMutator(this)

fun MyClass.mutate(mutation: Mutation<MyClass>): MyClass =
    mutator().apply(mutation).value

fun List<MyClass>.mutator(): ListMutator<MyClass> =
    mutator(child = { onChild -> mutator().apply { observe(onChild) } })

fun Set<MyClass>.mutator(): SetMutator<MyClass> =
    mutator(child = { onChild -> mutator().apply { observe(onChild) } })

var Mutator<MyClass>.name
    get() = value.name
    set(v) = modifyIfChanged(value.name, v) { it.copy(name = v) }

var Mutator<MyClass>.type
    get() = value.type
    set(v) = modifyIfChanged(value.type, v) { it.copy(type = v) }


////  TEST-CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

fun main() {

    Examples.dataClassExample()

    Examples.listExample()

    Examples.setExample()
}

object Examples {

    fun dataClassExample() {

        println("----------------------------------------------------------------------------------------")
        println("-- Simple Data Class example")

        val myInst = MyClass("Hallo!", MyEnum.One)

        val mutator = myInst.mutator()

        observe(mutator) { println("Value changed: $it") }

        println("Is modified: ${mutator.isModified}")
        mutator.name = "Hello World!"
        println("Is modified: ${mutator.isModified}")

        val prop = mutator::name
        prop.set("Hello Prop!")

        println(myInst.mutate { name = "Hallo mutate()" })

        println()
    }

    fun listExample() {
        println("----------------------------------------------------------------------------------------")
        println("-- List Mutator Example")

        val value = listOf(
            MyClass("first", MyEnum.One),
            MyClass("second", MyEnum.One),
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

        mutator.add(0, MyClass("Hello there!"))

        pos2.name = "I am happy again!"

        println(mutator.value)

        println("---------------------------------------------------------------------------------------")

        mutator.modify { it.map { e -> e.copy(name = "") }.toMutableList() }

        mutator[0].name = "I have a new name"

        println(mutator.value)

        println()
    }

    fun setExample() {
        println("----------------------------------------------------------------------------------------")
        println("-- Set Mutator Example")

        val input = setOf(
            MyClass("first", MyEnum.One),
            MyClass("second", MyEnum.One),
        )

        val mutator: SetMutator<MyClass> = input.mutator()
        observe(mutator) { it: Set<MyClass> -> println("Set changed: $it") }

        mutator.forEachIndexed { idx, it ->
            it.name = "Hello $idx!"
            it.type = MyEnum.Two
        }

        mutator.add(MyClass("Hello there!"))

        println(mutator.value)

        println("---------------------------------------------------------------------------------------")

        mutator.modify { it.mapIndexed { idx, e -> e.copy(name = "Idx $idx") }.toMutableSet() }

        mutator.first().name = "I have a new name"
//    println(mutator.value)

        println()
    }
}
