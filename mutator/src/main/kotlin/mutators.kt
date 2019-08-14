package de.peekandpoke.ultra.mutator

typealias OnModify<T> = (newValue: T) -> Unit

interface Mutator<I : Any> {
    fun getResult(): I

    fun isModified(): Boolean
}

abstract class MutatorBase<I : Any, R : I>(private val input: I, protected val onModify: OnModify<I>) : Mutator<I> {

    private var mutableResult: R? = null

    override fun isModified() = mutableResult != null

    override fun getResult(): I = mutableResult ?: input

    operator fun plusAssign(value: I) {

        if (getResult() !== value) {
            replaceResult(
                copy(value)
            )
        }
    }

    protected abstract fun copy(input: I): R

    protected fun getMutableResult(): R {

        return mutableResult ?: replaceResult(
            copy(input)
        )
    }

    private fun replaceResult(new: R): R {

        mutableResult = new

        onModify(new)

        return new
    }
}



