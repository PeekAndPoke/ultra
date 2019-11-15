package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.kMapType
import de.peekandpoke.ultra.common.kMutableMapType
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class MapAwakerSpec : StringSpec({

    "Awaking a Map must work" {

        val subject = MapAwaker.forMap(kMapType<String, Int>().type)

        val codec = Codec.default

        val result = subject.awake(mapOf("a" to 1, 2 to 2), codec.awakerContext)!!

        assertSoftly {
            Map::class.java.isAssignableFrom(result::class.java) shouldBe true
            result shouldBe mapOf("a" to 1, "2" to 2)
        }
    }

    "Awaking a Map must return null for invalid data" {

        val subject = MapAwaker.forMap(kMapType<String, Int>().type)

        val codec = Codec.default

        val result = subject.awake(listOf<Any>(), codec.awakerContext)

        assertSoftly {
            result shouldBe null
        }
    }

    "Awaking a Map with nullable values must work" {

        val subject = MapAwaker.forMap(kMapType<String, Int?>().type)

        val codec = Codec.default

        val result = subject.awake(mapOf("a" to 1, 2 to null), codec.awakerContext)!!

        assertSoftly {
            Map::class.java.isAssignableFrom(result::class.java) shouldBe true
            result shouldBe mapOf("a" to 1, "2" to null)
        }
    }

    "Awaking a Map with non-nullable values must fail when a null is found" {

        val subject = MapAwaker.forMap(kMapType<String, Int>().type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(mapOf("a" to 1, "wrong" to null), codec.awakerContext)
        }

        assertSoftly {
            error.message shouldContain "root.wrong[VAL]"
        }
    }

    "Awaking a Map (in a data class) must work" {

        data class DataClass(val items: Map<String, Int>)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("items" to mapOf("1" to "1", "2" to "2")))!!

        assertSoftly {
            Map::class.java.isAssignableFrom(result.items::class.java) shouldBe true
            result.items shouldBe mapOf("1" to 1, "2" to 2)
        }
    }

    "Awaking a MutableMap must work" {

        val subject = MapAwaker.forMap(kMutableMapType<String, Int>().type)

        val codec = Codec.default

        val result = subject.awake(mapOf("a" to 1, 2 to 2), codec.awakerContext)!!

        assertSoftly {
            MutableMap::class.java.isAssignableFrom(result::class.java) shouldBe true
            result shouldBe mapOf("a" to 1, "2" to 2)
        }
    }

    "Awaking a MutableMap with nullable values must work" {

        val subject = MapAwaker.forMap(kMutableMapType<String, Int?>().type)

        val codec = Codec.default

        val result = subject.awake(mapOf("a" to 1, 2 to null), codec.awakerContext)!!

        assertSoftly {
            MutableMap::class.java.isAssignableFrom(result::class.java) shouldBe true
            result shouldBe mapOf("a" to 1, "2" to null)
        }
    }

    "Awaking a MutableMap with non-nullable values must fail when a null is found" {

        val subject = MapAwaker.forMap(kMutableMapType<String, Int>().type)

        val codec = Codec.default

        val error = shouldThrow<AwakerException> {
            subject.awake(mapOf("a" to 1, "wrong" to null), codec.awakerContext)
        }

        assertSoftly {
            error.message shouldContain "root.wrong[VAL]"
        }
    }

    "Awaking a MutableMap (in a data class) must work" {

        data class DataClass(val items: MutableMap<String, Int>)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("items" to mapOf("1" to "1", "2" to "2")))!!

        assertSoftly {
            MutableMap::class.java.isAssignableFrom(result.items::class.java) shouldBe true
            result.items shouldBe mapOf("1" to 1, "2" to 2)
        }
    }

})
