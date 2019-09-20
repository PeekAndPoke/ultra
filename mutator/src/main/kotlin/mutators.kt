package de.peekandpoke.ultra.mutator

typealias OnModify<T> = (newValue: T) -> Unit

interface Mutator<I : Any> {

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

abstract class MutatorBase<I : Any, R : I>(

    private val inputValue: I,
    protected val onModify: OnModify<I>

) : Mutator<I> {

    private var mutableResult: R? = null

    override fun getInput(): I = inputValue

    override fun getResult(): I = mutableResult ?: inputValue

    override fun isModified() = mutableResult != null

    operator fun plusAssign(value: I) {

        if (getResult() !== value) {
            replaceResult(
                copy(value)
            )
        }
    }

    protected abstract fun copy(input: I): R

    protected fun getMutableResult(): R = mutableResult ?: replaceResult(copy(inputValue))

    private fun replaceResult(new: R): R = new.apply {
        // keep the modified result
        mutableResult = this
        // notify outer mutators
        onModify(this)
    }
}



