package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

/**
 * A non-@Mutable data class simulating an external type that should NOT get a mutator.
 */
data class ExternalType(
    val value: String,
)

/**
 * Another @Mutable(deep = false) class, to test nested shallow references.
 */
@Mutable(deep = false)
data class ShallowChild(
    val label: String,
)

/**
 * A @Mutable(deep = false) class that references both @Mutable types and non-@Mutable types.
 *
 * - Properties referencing types in the processing set (Address, ShallowChild) -> .mutator() accessor
 * - Properties referencing types NOT in the processing set (ExternalType) -> simple get/set
 * - Collections of types in the processing set -> collection mutator accessor
 * - Collections of types NOT in the processing set -> simple get/set
 */
@Mutable(deep = false)
data class ShallowOrder(
    val id: String,
    val meta: ExternalType,
    val optionalMeta: ExternalType?,
    val address: Address,
    val optionalAddress: Address?,
    val child: ShallowChild,
    val items: List<ExternalType>,
    val addresses: List<Address>,
    val tagSet: Set<ExternalType>,
    val addressSet: Set<Address>,
    val lookup: Map<String, ExternalType>,
    val addressMap: Map<String, Address>,
)
