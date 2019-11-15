package de.peekandpoke.ultra.mutator

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.javaField

open class DataClassMutator<T : Any>(input: T, onModify: OnModify<T>) : MutatorBase<T, T>(input, onModify) {

    fun <X> modify(property: KProperty0<X>, old: X, new: X) {

        val field = property.javaField

        if (field != null) {

            field.isAccessible = true

            // We only trigger the cloning, when the value has changed
            if (old.isNotSameAs(new)) {
                field.set(getMutableResult(), new)
            }
        }
    }

    override fun copy(input: T): T = Cloner.cloneDataClass(input)
}
