package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutate
import io.peekandpoke.mutator.domain.street

class MutatorBaseSpec : StringSpec({

    "get() returns the current value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.get() shouldBe address
    }

    "set() replaces the value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val newAddress = Address("New Street", "New City", "New Zip")

        mutator.set(newAddress)

        mutator.get() shouldBe newAddress
    }

    "invoke() without args returns the current value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator() shouldBe address
    }

    "invoke(value) sets the value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val newAddress = Address("New Street", "New City", "New Zip")

        mutator(newAddress)

        mutator() shouldBe newAddress
    }

    "isModified() returns false initially" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.isModified() shouldBe false
    }

    "isModified() returns true after modification" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.set(Address("Other", "City", "Zip"))

        mutator.isModified() shouldBe true
    }

    "isModified() returns false when value is set back to initial" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.set(Address("Other", "City", "Zip"))
        mutator.set(address)

        mutator.isModified() shouldBe false
    }

    "getInitialValue() returns the initial value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.getInitialValue() shouldBe address
    }

    "getInitialValue() still returns initial after modification" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.set(Address("Other", "City", "Zip"))

        mutator.getInitialValue() shouldBe address
    }

    "commit() resets isModified to false" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val newAddress = Address("New Street", "New City", "New Zip")

        mutator.set(newAddress)
        mutator.isModified() shouldBe true

        mutator.commit()
        mutator.isModified() shouldBe false
    }

    "getInitialValue() returns committed value after commit()" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val newAddress = Address("New Street", "New City", "New Zip")

        mutator.set(newAddress)
        mutator.commit()

        mutator.getInitialValue() shouldBe newAddress
    }

    "reset() reverts to initial value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()

        mutator.set(Address("Other", "City", "Zip"))
        mutator.isModified() shouldBe true

        mutator.reset()

        mutator.get() shouldBe address
        mutator.isModified() shouldBe false
    }

    "reset() after commit reverts to committed value" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val committed = Address("Committed", "City", "Zip")

        mutator.set(committed)
        mutator.commit()
        mutator.set(Address("Other", "City", "Zip"))

        mutator.reset()

        mutator.get() shouldBe committed
        mutator.isModified() shouldBe false
    }

    "reset() works on data class mutator via mutate" {
        val address = Address("Street", "City", "Zip")

        val result = address.mutate {
            street = "Changed"
            city = "Changed"
            reset()
        }

        result shouldBe address
    }

    "isModified() works on ListMutator" {
        val list = listOf(
            Address("Street 1", "City", "Zip"),
        )
        val mutator = list.mutator()

        mutator.isModified() shouldBe false
        mutator.add(Address("Street 2", "City", "Zip"))
        mutator.isModified() shouldBe true
    }

    "isModified() works on SetMutator" {
        val set = setOf(
            Address("Street 1", "City", "Zip"),
        )
        val mutator = set.mutator()

        mutator.isModified() shouldBe false
        mutator.add(Address("Street 2", "City", "Zip"))
        mutator.isModified() shouldBe true
    }

    "isModified() works on MapMutator" {
        val map = mapOf(
            "a" to Address("Street 1", "City", "Zip"),
        )
        val mutator = map.mutator()

        mutator.isModified() shouldBe false
        mutator.put("b", mutator.getChildMutator(Address("Street 2", "City", "Zip")))
        mutator.isModified() shouldBe true
    }

    "reset() works on ListMutator" {
        val list = listOf(
            Address("Street 1", "City", "Zip"),
        )
        val mutator = list.mutator()

        mutator.add(Address("Street 2", "City", "Zip"))
        mutator.isModified() shouldBe true

        mutator.reset()

        mutator.get() shouldBe list
        mutator.isModified() shouldBe false
    }

    "reset() works on SetMutator" {
        val set = setOf(
            Address("Street 1", "City", "Zip"),
        )
        val mutator = set.mutator()

        mutator.add(Address("Street 2", "City", "Zip"))
        mutator.isModified() shouldBe true

        mutator.reset()

        mutator.get() shouldBe set
        mutator.isModified() shouldBe false
    }

    "reset() works on MapMutator" {
        val map = mapOf(
            "a" to Address("Street 1", "City", "Zip"),
        )
        val mutator = map.mutator()

        mutator.put("b", mutator.getChildMutator(Address("Street 2", "City", "Zip")))
        mutator.isModified() shouldBe true

        mutator.reset()

        mutator.get() shouldBe map
        mutator.isModified() shouldBe false
    }

    "commit() then isModified() works on ListMutator" {
        val list = listOf(
            Address("Street 1", "City", "Zip"),
        )
        val mutator = list.mutator()

        mutator.add(Address("Street 2", "City", "Zip"))
        mutator.commit()
        mutator.isModified() shouldBe false

        mutator.add(Address("Street 3", "City", "Zip"))
        mutator.isModified() shouldBe true
    }

    "set() notifies observers" {
        val address = Address("Street", "City", "Zip")
        val mutator = address.mutator()
        val notifications = mutableListOf<Address>()

        mutator.onChange { notifications.add(it) }
        mutator.set(Address("New", "City", "Zip"))

        notifications.size shouldBe 1
        notifications[0].street shouldBe "New"
    }
})
