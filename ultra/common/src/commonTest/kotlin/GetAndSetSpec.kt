package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GetAndSetSpec : StringSpec({

    "get returns the current value" {
        var backing = 10
        val gas = GetAndSet.of(getter = { backing }, setter = { backing = it })

        gas.get() shouldBe 10
        gas() shouldBe 10
    }

    "set updates the value" {
        var backing = 10
        val gas = GetAndSet.of(getter = { backing }, setter = { backing = it })

        gas.set(20)

        gas.get() shouldBe 20
        backing shouldBe 20
    }

    "invoke(value) sets and returns the value" {
        var backing = 10
        val gas = GetAndSet.of(getter = { backing }, setter = { backing = it })

        val result = gas(42)

        result shouldBe 42
        gas() shouldBe 42
    }

    "equals compares by current value" {
        var a = 10
        var b = 10
        val gasA = GetAndSet.of(getter = { a }, setter = { a = it })
        val gasB = GetAndSet.of(getter = { b }, setter = { b = it })

        gasA shouldBe gasB

        b = 20
        gasA shouldNotBe gasB
    }

    "hashCode is based on current value" {
        var backing = 42
        val gas = GetAndSet.of(getter = { backing }, setter = { backing = it })

        gas.hashCode() shouldBe 42.hashCode()
    }

    "set via invoke operator updates the backing value" {
        var backing = "hello"
        val gas = GetAndSet.of(getter = { backing }, setter = { backing = it })

        gas("world")

        gas() shouldBe "world"
        backing shouldBe "world"
    }
    // Observable behaviour ////////////////////////////////////////////////////////////////////////

    "setting a value notifies observers" {
        var backing = 1
        val subject = GetAndSet.of(getter = { backing }, setter = { backing = it; it })
        val seen = mutableListOf<Int>()

        subject.observe { seen.add(it) }

        subject.set(42)
        subject.set(43)

        seen shouldBe listOf(42, 43)
    }

    "an unsubscribed observer stops receiving values" {
        var backing = 1
        val subject = GetAndSet.of(getter = { backing }, setter = { backing = it; it })
        val seen = mutableListOf<Int>()

        val unsub = subject.observe { seen.add(it) }
        subject.set(1)
        unsub()
        subject.set(2)

        seen shouldBe listOf(1)
    }

    "reading a value notifies nobody" {
        var backing = 1
        val subject = GetAndSet.of(getter = { backing }, setter = { backing = it; it })
        val seen = mutableListOf<Int>()

        subject.observe { seen.add(it) }
        subject.get()

        seen shouldBe emptyList()
    }
})
