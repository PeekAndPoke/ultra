package de.peekandpoke.slumberTestClasses

@ConsistentCopyVisibility
data class DataClassWithPrivateCtor private constructor(
    val str: String,
    val def: String? = null,
    val list: List<String> = emptyList(),
) {
    companion object {
        fun of(str: String) = DataClassWithPrivateCtor(str)
    }
}
