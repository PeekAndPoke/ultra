package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.playground.MyClassMutator.Companion.mutate
import de.peekandpoke.ultra.playground.MyClassMutator.Companion.mutator
import kotlin.reflect.KMutableProperty0

////  LIBRARY  ////////////////////////////////////////////////////////////////////////////////////////////////

typealias OnChange<T> = (newValue: T) -> Unit
typealias Mutation<T> = Mutator<T>.() -> Unit

interface Mutator<T> {

    abstract class Base<T>(
        private val initial: T,
        private val onChange: OnChange<T>,
    ) : Mutator<T> {
        private var _value = initial

        override val isModified: Boolean get() = value != initial
        override val value get() = _value

        fun <X> X.commit(): X {
            onChange(value)
            return this
        }

        override fun modify(block: (T) -> T) {
            block(_value)
                // Was the value changed?
                .takeIf { it != _value }
                // Yes store new value and call callback
                ?.let {
                    _value = it
                    commit()
                }
        }
    }

    class Null<T>(initial: T) : Base<T>(initial = initial, onChange = {})

    val value: T
    val isModified: Boolean
    fun modify(block: (T) -> T)
}

abstract class DataClassMutator<T : Any>(initial: T, onChange: OnChange<T>) :
    Mutator.Base<T>(initial, onChange)

class ListMutator<T>(
    initial: List<T>,
    onChange: OnChange<List<T>>,
    private val forward: T.(OnChange<T>) -> Mutator<T>,
) : Mutator.Base<MutableList<T>>(
    initial = initial.toMutableList(),
    onChange = { onChange(it.toList()) },
), MutableList<Mutator<T>> {
    /**
     * Iterator impl
     */
    internal inner class It(
        startIndex: Int = 0,
    ) : MutableListIterator<Mutator<T>> {

        private var pos = startIndex
        private var current: Mutator<T>? = null

        override fun nextIndex(): Int = pos

        override fun hasNext(): Boolean = pos < value.size

        override fun next(): Mutator<T> {
            val idx = pos++

            return value[idx].run {
                // return the current element mapped to a mutator with onModify callback
                forward(this) { setAt(idx, it) }.also {
                    // remember the current element, so we can use it for remove()
                    current = it
                }
            }
        }

        override fun hasPrevious(): Boolean = pos > 0

        override fun previousIndex(): Int = pos - 1

        override fun previous(): Mutator<T> {
            TODO("Not yet implemented")
        }

        override fun remove() {
            TODO("Not yet implemented")
        }

        override fun add(element: Mutator<T>) {
            TODO("Not yet implemented")
        }

        override fun set(element: Mutator<T>) {
            TODO("Not yet implemented")
        }
    }

    /**
     * The size of the currently mutated list.
     */
    override val size: Int get() = value.size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = value.isEmpty()

    /**
     * Returns true if the list contains the given [element]
     */
    override fun contains(element: Mutator<T>) = value.contains(element.value)

    /**
     * Returns true if the list contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<T>>) = value.containsAll(elements.map { it.value })

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun indexOf(element: Mutator<T>) = value.indexOf(element.value)

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun lastIndexOf(element: Mutator<T>) = value.lastIndexOf(element.value)

    /**
     * Clears the whole list
     */
    override fun clear() {
        modify { mutableListOf() }.commit()
    }

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: Mutator<T>): Boolean {
        return value.add(element.value).commit()
    }

    /**
     * Inserts an element into the list at the specified [index].
     */
    override fun add(index: Int, element: Mutator<T>) {
        return value.add(index, element.value).commit()
    }

    /**
     * Adds all the elements of the specified collection to the end of this list.
     */
    override fun addAll(elements: Collection<Mutator<T>>): Boolean {
        return value.addAll(elements.map { it.value }).commit()
    }

    /**
     * Inserts all the elements of the specified collection [elements] into this list at the specified [index].
     */
    override fun addAll(index: Int, elements: Collection<Mutator<T>>): Boolean {
        return value.addAll(index, elements.map { it.value }).commit()
    }

    /**
     * Removes the given element
     *
     * @return true when the element has been removed from the list
     */
    override fun remove(element: Mutator<T>): Boolean {
        return value.remove(element.value).commit()
    }

    /**
     * Removes an element at the specified [index] from the list.
     *
     * NOTICE: modifying the returned element will have no effect
     *
     * @return the element that has been removed.
     */
    override fun removeAt(index: Int): Mutator<T> {
        return value.removeAt(index).forward { }.commit()
    }

    /**
     * Removes all of the given [elements]
     */
    override fun removeAll(elements: Collection<Mutator<T>>): Boolean {
        return value.removeAll(elements.map { it.value }).commit()
    }

    /**
     * Retains all of the given [elements]
     */
    override fun retainAll(elements: Collection<Mutator<T>>): Boolean {
        return value.retainAll(elements.map { it.value }).commit()
    }

    /**
     * Get the value at the given index
     */
    override fun get(index: Int): Mutator<T> {
        var last = value[index]

        return value[index].forward { new ->
            // Is our element still in the list and where is it?
            val pos = value.indexOfFirst { it === last }

            if (pos != -1) {
                last = new
                setAt(pos, new)
            }
        }
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * Returns: the element previously at the specified position.
     */
    override fun set(index: Int, element: Mutator<T>): Mutator<T> {
        return value.set(index, element.value).forward { }.commit()
    }

    /**
     * Returns an iterator
     */
    override fun iterator(): MutableIterator<Mutator<T>> {
        return It()
    }

    /**
     * Returns a list iterator
     */
    override fun listIterator(): MutableListIterator<Mutator<T>> {
        return It()
    }

    /**
     * Returns a list iterator starting at the specified [index].
     */
    override fun listIterator(index: Int): MutableListIterator<Mutator<T>> {
        return It(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutator<T>> {
        TODO("Not yet implemented")
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setAt(index: Int, element: T) {
        value[index] = element
        commit()
    }
}

fun <T> ListMutator<T>.add(element: T): Boolean = add(Mutator.Null(element))

fun <T> ListMutator<T>.add(index: Int, element: T) = add(index, Mutator.Null(element))


////  USER CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

enum class MyEnum {
    One,
    Two,
}

data class MyClass(val name: String, val type: MyEnum = MyEnum.One)

////  GENERATED CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

class MyClassMutator(
    initial: MyClass,
    onChange: OnChange<MyClass>,
) : DataClassMutator<MyClass>(initial, onChange) {
    companion object {
        fun MyClass.mutate(mutation: Mutation<MyClass>): MyClass =
            mutator(onChange = { _: MyClass -> }).apply(mutation).value

        fun MyClass.mutator(onChange: OnChange<MyClass> = {}): MyClassMutator =
            MyClassMutator(this, onChange)

        fun List<MyClass>.mutator(onChange: OnChange<List<MyClass>>): ListMutator<MyClass> =
            ListMutator(this, onChange) { onChildChange -> mutator(onChildChange) }
    }
}

var Mutator<MyClass>.name get() = value.name; set(v) = modify { it.copy(name = v) }
var Mutator<MyClass>.type get() = value.type; set(v) = modify { it.copy(type = v) }


////  TEST-CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

fun main() {

    // Simple Data Class example -----------------------------------------------------------
    val myInst = MyClass("Hallo!", MyEnum.One)

    val mutator = myInst.mutator { println("Value changed: $it") }
    println("Is modified: ${mutator.isModified}")
    mutator.name = "Hello World!"
    println("Is modified: ${mutator.isModified}")

    val prop: KMutableProperty0<String> = mutator::name
    prop.set("Hello Prop!")

    println(myInst.mutate { name = "Hallo mutate()" })

    // List of Simple Data Class example -----------------------------------------------------------

    val myList = listOf(
        MyClass("first", MyEnum.One),
        MyClass("second", MyEnum.One),
    )

    val listMutator: ListMutator<MyClass> = myList.mutator { it: List<MyClass> -> println("List changed: $it") }

    listMutator.forEachIndexed { idx, it ->
        it.name = "Hello $idx!"
        it.type = MyEnum.Two
    }

    val pos2 = listMutator[1]
    listMutator.removeAt(0)
    pos2.name = "I am so alone"

    listMutator.add(0, MyClass("Hello there!"))

    pos2.name = "I am happy again!"

    println(listMutator.value)
}
