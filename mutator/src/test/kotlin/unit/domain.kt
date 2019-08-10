package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.OnModify

data class Wrapper<X>(val value: X, val onModify: OnModify<X>)

data class SomeDataClass(val aString: String, val anInt: Int)

data class ClassWithList(val list: List<SomeDataClass>)
