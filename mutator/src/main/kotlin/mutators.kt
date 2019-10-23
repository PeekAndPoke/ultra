package de.peekandpoke.ultra.mutator

typealias OnModify<T> = (newValue: T) -> Unit

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

/**
 * Base implementation for all mutators
 */
abstract class MutatorBase<I, R : I>(
    /** The value that the mutator is working on */
    private val inputValue: I,
    /** A callback that will be invoked when the [inputValue] is replaced or modified */
    protected val onModify: OnModify<I>
) : Mutator<I> {

    /**
     * The mutable result.
     *
     * It will be initialized only when it is needed.
     */
    private var mutableResult: R? = null

    /**
     * Returns the [inputValue] of the mutator
     */
    override fun getInput(): I = inputValue

    /**
     * Returns the result, which is
     * - the [inputValue] when no modifications occurred
     * - or the [mutableResult] when at least one modification occurred
     */
    override fun getResult(): I = mutableResult ?: inputValue

    /**
     * Returns true when the [mutableResult] is created, which should mean that at least one modification was done.
     */
    override fun isModified() = mutableResult != null

    /**
     * Replaces the whole value that the mutator is working on
     */
    operator fun plusAssign(value: I) {

        if (getResult() !== value) {
            replaceResult(
                copy(value)
            )
        }
    }

    /**
     * Must return a mutable copy of the given [input]
     */
    protected abstract fun copy(input: I): R

    /**
     * Gets the mutable result
     *
     * If the mutable result is not yet created it will be created from the [inputValue]
     */
    protected fun getMutableResult(): R = mutableResult ?: replaceResult(copy(inputValue))

    /**
     * Replaces the mutable result and invokes [onModify] with the [new] value
     */
    private fun replaceResult(new: R): R = new.apply {
        // keep the modified result
        mutableResult = this
        // notify outer mutators
        onModify(this)
    }
}



