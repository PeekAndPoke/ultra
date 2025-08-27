package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.playground.lib.DataClassMutator
import de.peekandpoke.ultra.playground.lib.ListMutator
import de.peekandpoke.ultra.playground.lib.Mutation
import de.peekandpoke.ultra.playground.lib.Mutator
import de.peekandpoke.ultra.playground.lib.SetMutator
import de.peekandpoke.ultra.playground.lib.mutator
import de.peekandpoke.ultra.playground.lib.onChange

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

inline val Mutator<MyClass>.address: DataClassMutator<MyAddress>
    get() = get().address.mutator()
        .onChange { address -> modify { get().copy(address = address) } }

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
