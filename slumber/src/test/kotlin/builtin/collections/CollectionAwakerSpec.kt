package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class CollectionAwakerSpec : StringSpec({

    "Awaking a List must work" {

        val subject = CollectionAwaker.forList(TypeRef.Int.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2), codec.secondPassAwakerContext)!!

        List::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe listOf(1, 2)
    }

    "Awaking a List of nullables must work" {

        val subject = CollectionAwaker.forList(TypeRef.Int.nullable.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)!!

        List::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe listOf(1, 2, null)
    }

    "Awaking a List of non-nullables must fail, when a null is found" {

        val subject = CollectionAwaker.forList(TypeRef.Int.list.type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)
        }

        // should contain the error for the correct position
        error.message shouldContain "root.2"
    }

    "Awaking a List (in a data class) must work" {

        data class DataClass(val items: List<Int>)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("items" to listOf("1", 2)))!!

        List::class.java.isAssignableFrom(result.items::class.java) shouldBe true
        result.items shouldBe listOf(1, 2)
    }

    "Awaking a MutableList must work" {

        val subject = CollectionAwaker.forList(TypeRef.Int.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2), codec.secondPassAwakerContext)!!

        result.shouldBeInstanceOf<MutableList<*>>()

        MutableList::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe listOf(1, 2)
    }

    "Awaking a MutableList of nullables must work" {

        val subject = CollectionAwaker.forList(TypeRef.Int.nullable.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)!!

        result.shouldBeInstanceOf<MutableList<*>>()

        MutableList::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe listOf(1, 2, null)
    }

    "Awaking a MutableList of non-nullables must fail, when a null is found" {

        val subject = CollectionAwaker.forList(TypeRef.Int.list.type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)
        }

        // should contain the error for the correct position
        error.message shouldContain "root.2"
    }

    "Awaking a MutableList (in a data class) must work" {

        data class DataClass(
            val items: List<Int>,
            val mutable: MutableList<Int>
        )

        val codec = Codec.default

        val result = codec.awake(
            DataClass::class, mapOf(
                "items" to listOf("1", 2),
                "mutable" to listOf("3", 4),
            )
        )!!

        result.items.shouldBeInstanceOf<List<*>>()
        result.mutable.shouldBeInstanceOf<MutableList<*>>()

        MutableList::class.java.isAssignableFrom(result.items::class.java) shouldBe true
        result.items shouldBe listOf(1, 2)

        result.mutable.apply { add(5) } shouldBe listOf(3, 4, 5)
    }

    "Awaking an empty MutableList (in a data class) must work" {

        data class DataClass(
            val items: List<Int>,
            val mutable: MutableList<Int>
        )

        val codec = Codec.default

        val result = codec.awake(
            DataClass::class, mapOf(
                "items" to listOf("1", 2),
                "mutable" to listOf(),
            )
        )!!

        result.items.shouldBeInstanceOf<List<*>>()
        result.mutable.shouldBeInstanceOf<MutableList<*>>()

        MutableList::class.java.isAssignableFrom(result.items::class.java) shouldBe true
        result.items shouldBe listOf(1, 2)

        result.mutable.apply { add(5) } shouldBe listOf(5)
    }

    "Awaking a Set must work" {

        val subject = CollectionAwaker.forSet(TypeRef.Int.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2), codec.secondPassAwakerContext)!!

        Set::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe setOf(1, 2)
    }

    "Awaking a Set of nullables must work" {

        val subject = CollectionAwaker.forSet(TypeRef.Int.nullable.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)!!

        Set::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe setOf(1, 2, null)
    }

    "Awaking a Set of non-nullables must fail, when a null is found" {

        val subject = CollectionAwaker.forSet(TypeRef.Int.list.type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)
        }

        // should contain the error for the correct position
        error.message shouldContain "root.2"
    }

    "Awaking a Set (in a data class) must work" {

        data class DataClass(val items: Set<Int>)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("items" to listOf("1", 2)))!!

        Set::class.java.isAssignableFrom(result.items::class.java) shouldBe true
        result.items shouldBe setOf(1, 2)
    }

    "Awaking a MutableSet must work" {

        val subject = CollectionAwaker.forSet(TypeRef.Int.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2), codec.secondPassAwakerContext)!!

        result.shouldBeInstanceOf<MutableSet<*>>()

        MutableSet::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe mutableSetOf(1, 2)
    }

    "Awaking a MutableSet of nullables must work" {

        val subject = CollectionAwaker.forSet(TypeRef.Int.nullable.list.type)

        val codec = Codec.default

        val result = subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)!!

        result.shouldBeInstanceOf<MutableSet<*>>()

        MutableSet::class.java.isAssignableFrom(result::class.java) shouldBe true
        result shouldBe mutableSetOf(1, 2, null)
    }

    "Awaking a MutableSet of non-nullables must fail, when a null is found" {

        val subject = CollectionAwaker.forList(TypeRef.Int.list.type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(listOf("1", 2, null), codec.secondPassAwakerContext)
        }

        // should contain the error for the correct position
        error.message shouldContain "root.2"
    }

    "Awaking a MutableSet (in a data class) must work" {

        data class DataClass(val items: MutableSet<Int>)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("items" to listOf("1", 2)))!!

        result.items.shouldBeInstanceOf<MutableSet<*>>()

        MutableSet::class.java.isAssignableFrom(result.items::class.java) shouldBe true
        result.items shouldBe setOf(1, 2)
    }
})
