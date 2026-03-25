package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.typeOf

class ReflectionSpec : StringSpec({

    "isPrimitive must be true for all primitive types" {
        typeOf<Boolean>().isPrimitive shouldBe true
        typeOf<Char>().isPrimitive shouldBe true
        typeOf<Byte>().isPrimitive shouldBe true
        typeOf<Short>().isPrimitive shouldBe true
        typeOf<Int>().isPrimitive shouldBe true
        typeOf<Long>().isPrimitive shouldBe true
        typeOf<Float>().isPrimitive shouldBe true
        typeOf<Double>().isPrimitive shouldBe true
    }

    "isPrimitive must be true for nullable primitive types" {
        typeOf<Int?>().isPrimitive shouldBe true
        typeOf<Boolean?>().isPrimitive shouldBe true
    }

    "isPrimitive must be false for non-primitive types" {
        typeOf<String>().isPrimitive shouldBe false
        typeOf<List<Int>>().isPrimitive shouldBe false
        typeOf<Any>().isPrimitive shouldBe false
    }

    "isPrimitiveOrString must be true for String" {
        typeOf<String>().isPrimitiveOrString shouldBe true
    }

    "isPrimitiveOrString must be true for all primitives" {
        typeOf<Int>().isPrimitiveOrString shouldBe true
        typeOf<Boolean>().isPrimitiveOrString shouldBe true
        typeOf<Double>().isPrimitiveOrString shouldBe true
    }

    "isPrimitiveOrString must be false for non-primitive non-String types" {
        typeOf<List<Int>>().isPrimitiveOrString shouldBe false
        typeOf<Any>().isPrimitiveOrString shouldBe false
        typeOf<Map<String, Int>>().isPrimitiveOrString shouldBe false
    }
})
