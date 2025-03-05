package de.peekandpoke.ultra.playground.lib

import de.peekandpoke.ultra.common.OnChange
import de.peekandpoke.ultra.common.toggle

fun <V> Set<V>.mutator(child: V.(OnChange<V>) -> Mutator<V>): SetMutator<V> = SetMutator(this, child)

fun <V> SetMutator<V>.add(element: V) = add(Mutator.Null(element))

fun <V> SetMutator<V>.remove(element: V) = remove(Mutator.Null(element))

fun <V> SetMutator<V>.toggle(element: V) = toggle(Mutator.Null(element))

class SetMutator<V>(initial: Set<V>, private val child: V.(OnChange<V>) -> Mutator<V>) :
    Mutator.Base<MutableSet<V>>(initial.toMutableSet()),
    MutableSet<Mutator<V>> {

    /**
     * Iterator impl
     */
    private inner class It : MutableIterator<Mutator<V>> {

        // We make a copy of the value, so we can modify while iteration
        private val inner = value.toList().iterator()
        private var current: V? = null

        override fun hasNext() = inner.hasNext()

        override fun next(): Mutator<V> {

            return inner.next().run {
                // remember the current element, so we can use it for remove()
                current = this

                // Keep track if the last state of our element, so we can remove and add correctly
                var last = this

                child(this) {
                    replace(last, it)
                    last = it
                }
            }
        }

        override fun remove() {
            current?.let {
                value.remove(it)
            }
        }
    }

    /**
     * Returns the size of the list
     */
    override val size get() = value.size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = value.isEmpty()

    /**
     * Returns true if the set contains the given [element]
     */
    override fun contains(element: Mutator<V>) = value.contains(element.value)

    /**
     * Returns true if the set contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<V>>) = value.containsAll(elements.extract())

    /**
     * Clears the whole list
     */
    override fun clear() {
        modify { it.also { clear() } }.commit()
    }

    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    override fun add(element: Mutator<V>): Boolean {
        return value.add(element.value).commit()
    }

    /**
     * Adds all [elements] to the set
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return value.addAll(elements.extract()).commit()
    }

    /**
     * Removes an [element] from the set
     *
     * @return 'true' of the element was removed from the set
     */
    override fun remove(element: Mutator<V>): Boolean {
        return value.remove(element.value).commit()
    }

    /**
     * Removes the specified [elements] from the list
     *
     * @return 'true' when the list has been modified
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        return value.removeAll(elements.extract()).commit()
    }

    /**
     * Retains all of the given [elements]
     *
     * @return 'true' when the list has been modified
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        return value.retainAll(elements.extract()).commit()
    }

    /**
     * Gets an iterator for the set
     */
    override fun iterator(): MutableIterator<Mutator<V>> {
        return It()
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun replace(old: V, new: V) {
        value.remove(old)
        value.add(new)
        commit()
    }

    /**
     * Extracts the values of the given mutators
     */
    private fun Collection<Mutator<V>>.extract(): Set<V> = map { it.value }.toSet()
}
