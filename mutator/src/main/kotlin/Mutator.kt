package de.peekandpoke.ultra.mutator

/**
 * Base interface for all mutators
 */
interface Mutator<I> {

    /**
     * Get the original input value that the mutator is working on
     */
    fun getInput(): I

    /**
     * Get the result of the mutation
     */
    fun getResult(): I

    /**
     * Returns true when any mutation has taken place
     */
    fun isModified(): Boolean
}
