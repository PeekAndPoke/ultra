package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.ExternalType
import io.peekandpoke.mutator.domain.ShallowChild
import io.peekandpoke.mutator.domain.ShallowOrder
import io.peekandpoke.mutator.domain.address
import io.peekandpoke.mutator.domain.addressMap
import io.peekandpoke.mutator.domain.addressSet
import io.peekandpoke.mutator.domain.addresses
import io.peekandpoke.mutator.domain.child
import io.peekandpoke.mutator.domain.id
import io.peekandpoke.mutator.domain.items
import io.peekandpoke.mutator.domain.label
import io.peekandpoke.mutator.domain.lookup
import io.peekandpoke.mutator.domain.meta
import io.peekandpoke.mutator.domain.mutate
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.optionalAddress
import io.peekandpoke.mutator.domain.optionalMeta
import io.peekandpoke.mutator.domain.street
import io.peekandpoke.mutator.domain.tagSet

class ShallowMutableSpec : StringSpec() {

    private val testAddress = Address(street = "Main St", city = "Springfield", zip = "12345")

    private val sampleOrder = ShallowOrder(
        id = "order-1",
        meta = ExternalType(value = "metadata"),
        optionalMeta = null,
        address = testAddress,
        optionalAddress = null,
        child = ShallowChild(label = "child-1"),
        items = listOf(ExternalType("item-1"), ExternalType("item-2")),
        addresses = listOf(testAddress),
        tagSet = emptySet(),
        addressSet = emptySet(),
        lookup = emptyMap(),
        addressMap = emptyMap(),
    )

    init {
        // Empty mutation returns same instance
        "empty mutation returns the exact same object" {
            val result = sampleOrder.mutate { }
            result shouldBeSameInstanceAs sampleOrder
        }

        // Simple get/set for primitive field
        "id (String) gets simple get/set" {
            val result = sampleOrder.mutate { id = "order-2" }
            result.id shouldBe "order-2"
            result shouldNotBe sampleOrder
        }

        // Simple get/set for ExternalType (not @Mutable, not in processing set)
        "meta (ExternalType) gets simple get/set" {
            val newMeta = ExternalType(value = "updated")
            val result = sampleOrder.mutate { meta = newMeta }
            result.meta shouldBe newMeta
        }

        // Simple get/set for nullable ExternalType
        "optionalMeta (ExternalType?) gets simple get/set - null to value" {
            val result = sampleOrder.mutate { optionalMeta = ExternalType("now-set") }
            result.optionalMeta shouldBe ExternalType("now-set")
        }

        "optionalMeta (ExternalType?) gets simple get/set - value to null" {
            val order = sampleOrder.copy(optionalMeta = ExternalType("has-value"))
            val result = order.mutate { optionalMeta = null }
            result.optionalMeta shouldBe null
        }

        // .mutator() accessor for Address (@Mutable, in processing set)
        "address (Address) gets mutator accessor - deep mutation" {
            val result = sampleOrder.mutate {
                address.street = "Oak Ave"
            }
            result.address.street shouldBe "Oak Ave"
            result.address.city shouldBe "Springfield"
        }

        // Nullable @Mutable type gets ?.mutator() accessor
        "optionalAddress (Address?) gets mutator accessor when non-null" {
            val order = sampleOrder.copy(optionalAddress = testAddress)
            val result = order.mutate {
                optionalAddress?.street = "Changed"
            }
            result.optionalAddress?.street shouldBe "Changed"
        }

        "optionalAddress (Address?) is null-safe when null" {
            val result = sampleOrder.mutate { }
            result.optionalAddress shouldBe null
        }

        // .mutator() accessor for ShallowChild (also @Mutable(deep=false), in processing set)
        "child (ShallowChild, also deep=false) gets mutator accessor" {
            val result = sampleOrder.mutate {
                child.label = "changed"
            }
            result.child.label shouldBe "changed"
        }

        // Simple get/set for List<ExternalType>
        "items (List<ExternalType>) gets simple get/set" {
            val newItems = listOf(ExternalType("new-item"))
            val result = sampleOrder.mutate { items = newItems }
            result.items shouldBe newItems
        }

        // .mutator() accessor for List<Address>
        "addresses (List<Address>) gets collection mutator accessor" {
            val result = sampleOrder.mutate {
                addresses[0].street = "Elm St"
            }
            result.addresses[0].street shouldBe "Elm St"
        }

        // Simple get/set for Set<ExternalType>
        "tagSet (Set<ExternalType>) gets simple get/set" {
            val newTags = setOf(ExternalType("tag"))
            val result = sampleOrder.mutate { tagSet = newTags }
            result.tagSet shouldBe newTags
        }

        // .mutator() accessor for Set<Address>
        "addressSet (Set<Address>) gets collection mutator accessor" {
            val order = sampleOrder.copy(addressSet = setOf(testAddress))
            val result = order.mutate {
                addressSet.first().street = "SetStreet"
            }
            result.addressSet.first().street shouldBe "SetStreet"
        }

        // Simple get/set for Map<String, ExternalType>
        "lookup (Map<String, ExternalType>) gets simple get/set" {
            val newLookup = mapOf("k" to ExternalType("v"))
            val result = sampleOrder.mutate { lookup = newLookup }
            result.lookup shouldBe newLookup
        }

        // .mutator() accessor for Map<String, Address>
        "addressMap (Map<String, Address>) gets collection mutator accessor" {
            val order = sampleOrder.copy(addressMap = mapOf("home" to testAddress))
            val result = order.mutate {
                addressMap["home"]!!.street = "MapStreet"
            }
            result.addressMap["home"]?.street shouldBe "MapStreet"
        }

        // isModified tracking works for shallow fields
        "isModified is false when meta is set to the same value" {
            val mutator = sampleOrder.mutator()
            mutator.meta = sampleOrder.meta
            mutator.isModified() shouldBe false
        }

        "isModified is true when meta is set to a different value" {
            val mutator = sampleOrder.mutator()
            mutator.meta = ExternalType("different")
            mutator.isModified() shouldBe true
        }
    }
}
