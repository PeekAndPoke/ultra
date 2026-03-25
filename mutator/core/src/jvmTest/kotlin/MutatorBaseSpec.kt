package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator

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
