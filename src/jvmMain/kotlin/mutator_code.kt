package de.peekandpoke.ultra.playground

import de.peekandpoke.mutator.DataClassMutator
import de.peekandpoke.mutator.ListMutator
import de.peekandpoke.mutator.Mutation
import de.peekandpoke.mutator.Mutator
import de.peekandpoke.mutator.SetMutator
import de.peekandpoke.mutator.mutator
import de.peekandpoke.mutator.onChange

// //  USER CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

enum class MyEnum {
    One,
    Two,
}

data class MyAddress(val street: String, val city: String)

data class MyClass(
    val name: String,
    val address: MyAddress = MyAddress("street", "city"),
    val type: MyEnum = MyEnum.One,
)

// //  GENERATED CODE  ////////////////////////////////////////////////////////////////////////////////////////////////

fun MyClass.mutator(): DataClassMutator<MyClass> = DataClassMutator(this)

fun MyClass.mutate(mutation: Mutation<MyClass>): MyClass = mutator().apply(mutation).get()

inline var Mutator<MyClass>.name
    get() = get().name
    set(v) = modifyIfChanged(get().name, v) { it.copy(name = v) }

inline var Mutator<MyClass>.type
    get() = get().type
    set(v) = modifyIfChanged(get().type, v) { it.copy(type = v) }

inline val Mutator<MyClass>.address
    get() = get().address.mutator()
        .onChange { address -> modifyValue { get().copy(address = address) } }

inline fun MyAddress.mutator(): DataClassMutator<MyAddress> = DataClassMutator(this)

inline fun MyAddress.mutate(mutation: Mutation<MyAddress>): MyAddress = mutator().apply(mutation).get()

inline var Mutator<MyAddress>.street
    get() = get().street
    set(v) = modifyIfChanged(get().street, v) { it.copy(street = v) }

inline var Mutator<MyAddress>.city
    get() = get().city
    set(v) = modifyIfChanged(get().city, v) { it.copy(city = v) }

inline fun List<MyClass>.mutator(): ListMutator<MyClass> = mutator(child = { mutator() })

inline fun Set<MyClass>.mutator(): SetMutator<MyClass> = mutator(child = { mutator() })
