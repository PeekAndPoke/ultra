package de.peekandpoke.slumber_test_classes

@Suppress("DataClassPrivateConstructor")
data class DataClassWithPrivateCtor private constructor(
    val str: String,
    val def: String? = null,
    val list: List<String> = emptyList(),
) {
    companion object {
        fun of(str: String) = DataClassWithPrivateCtor(str)
    }
}
