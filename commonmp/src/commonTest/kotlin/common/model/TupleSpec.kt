package de.peekandpoke.ultra.common.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TupleSpec : StringSpec({

    /*---------------------------- Tuple1 Test Cases ----------------------------*/

    "Tuple1: should return correct value" {
        val tuple = Tuple1(1)
        tuple.e1 shouldBe 1
    }
    "Tuple1: asList should return correctly" {
        val tuple = Tuple1(1)
        tuple.asList shouldBe listOf(1)
    }
    "Tuple1: plus should return correctly" {
        val tuple = Tuple1(1)
        val expectedTuple = Tuple2(1, 2.2)
        (tuple + 2.2).shouldBe(expectedTuple)
    }
    "Tuple1: append should return correctly" {
        val tuple = Tuple1(1)
        val expectedTuple = Tuple2(1, "a")
        tuple.append("a").shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple2 Test Cases ----------------------------*/

    "Tuple2: should return correct values" {
        val tuple = Tuple2(1, "a")
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
    }
    "Tuple2: asList should return correctly" {
        val tuple = Tuple2(1, "a")
        tuple.asList shouldBe listOf(1, "a")
    }
    "Tuple2: plus should return correctly" {
        val tuple = Tuple2(1, "a")
        val expectedTuple = Tuple3(1, "a", 2.2)
        (tuple + 2.2).shouldBe(expectedTuple)
    }
    "Tuple2: append should return correctly" {
        val tuple = Tuple2(1, "a")
        val expectedTuple = Tuple3(1, "a", true)
        tuple.append(true).shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple3 Test Cases ----------------------------*/

    "Tuple3: should return correct values" {
        val tuple = Tuple3(1, "a", 2.2)
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
    }
    "Tuple3: asList should return correctly" {
        val tuple = Tuple3(1, "a", 2.2)
        tuple.asList shouldBe listOf(1, "a", 2.2)
    }
    "Tuple3: plus should return correctly" {
        val tuple = Tuple3(1, "a", 2.2)
        val expectedTuple = Tuple4(1, "a", 2.2, true)
        (tuple + true).shouldBe(expectedTuple)
    }
    "Tuple3: append should return correctly" {
        val tuple = Tuple3(1, "a", 2.2)
        val expectedTuple = Tuple4(1, "a", 2.2, true)
        tuple.append(true).shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple4 Test Cases ----------------------------*/

    "Tuple4: should return correct values" {
        val tuple = Tuple4(1, "a", 2.2, true)
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
    }
    "Tuple4: asList should return correctly" {
        val tuple = Tuple4(1, "a", 2.2, true)
        tuple.asList shouldBe listOf(1, "a", 2.2, true)
    }
    "Tuple4: plus should return correctly" {
        val tuple = Tuple4(1, "a", 2.2, true)
        val expectedTuple = Tuple5(1, "a", 2.2, true, "b")
        (tuple + "b").shouldBe(expectedTuple)
    }
    "Tuple4: append should return correctly" {
        val tuple = Tuple4(1, "a", 2.2, true)
        val expectedTuple = Tuple5(1, "a", 2.2, true, "b")
        tuple.append("b").shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple5 Test Cases ----------------------------*/

    "Tuple5: should return correct values" {
        val tuple = Tuple5(1, "a", 2.2, true, "b")
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
    }
    "Tuple5: asList should return correctly" {
        val tuple = Tuple5(1, "a", 2.2, true, "b")
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b")
    }
    "Tuple5: plus should return correctly" {
        val tuple = Tuple5(1, "a", 2.2, true, "b")
        val expectedTuple = Tuple6(1, "a", 2.2, true, "b", "c")
        (tuple + "c").shouldBe(expectedTuple)
    }
    "Tuple5: append should return correctly" {
        val tuple = Tuple5(1, "a", 2.2, true, "b")
        val expectedTuple = Tuple6(1, "a", 2.2, true, "b", "c")
        tuple.append("c").shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple6 Test Cases ----------------------------*/

    "Tuple6: should return correct values" {
        val tuple = Tuple6(1, "a", 2.2, true, "b", "c")
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
        tuple.e6 shouldBe "c"
    }
    "Tuple6: asList should return correctly" {
        val tuple = Tuple6(1, "a", 2.2, true, "b", "c")
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b", "c")
    }

    "Tuple6: plus should return correctly" {
        val tuple = Tuple6(1, "a", 2.2, true, "b", "c")
        val expectedTuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        (tuple + 42).shouldBe(expectedTuple)
    }
    "Tuple6: append should return correctly" {
        val tuple = Tuple6(1, "a", 2.2, true, "b", "c")
        val expectedTuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        tuple.append(42).shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple7 Test Cases ----------------------------*/

    "Tuple7: should return correct values" {
        val tuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
        tuple.e6 shouldBe "c"
        tuple.e7 shouldBe 42
    }
    "Tuple7: asList should return correctly" {
        val tuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b", "c", 42)
    }

    "Tuple7: plus should return correctly" {
        val tuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        val expectedTuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        (tuple + false).shouldBe(expectedTuple)
    }
    "Tuple7: append should return correctly" {
        val tuple = Tuple7(1, "a", 2.2, true, "b", "c", 42)
        val expectedTuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        tuple.append(false).shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple8 Test Cases ----------------------------*/

    "Tuple8: should return correct values" {
        val tuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
        tuple.e6 shouldBe "c"
        tuple.e7 shouldBe 42
        tuple.e8 shouldBe false
    }
    "Tuple8: asList should return correctly" {
        val tuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b", "c", 42, false)
    }
    "Tuple8: plus should return correctly" {
        val tuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        val expectedTuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "d")
        (tuple + "d").shouldBe(expectedTuple)
    }
    "Tuple8: append should return correctly" {
        val tuple = Tuple8(1, "a", 2.2, true, "b", "c", 42, false)
        val expectedTuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "d")
        tuple.append("d").shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple9 Test Cases ----------------------------*/

    "Tuple9: should return correct values" {
        val tuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "e")
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
        tuple.e6 shouldBe "c"
        tuple.e7 shouldBe 42
        tuple.e8 shouldBe false
        tuple.e9 shouldBe "e"
    }
    "Tuple9: asList should return correctly" {
        val tuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "e")
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b", "c", 42, false, "e")
    }
    "Tuple9: plus should return correctly" {
        val tuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "e")
        val expectedTuple = Tuple10(1, "a", 2.2, true, "b", "c", 42, false, "e", "d")
        (tuple + "d").shouldBe(expectedTuple)
    }
    "Tuple9: append should return correctly" {
        val tuple = Tuple9(1, "a", 2.2, true, "b", "c", 42, false, "e")
        val expectedTuple = Tuple10(1, "a", 2.2, true, "b", "c", 42, false, "e", "d")
        tuple.append("d").shouldBe(expectedTuple)
    }

    /*---------------------------- Tuple10 Test Cases ----------------------------*/

    "Tuple10: should return correct values" {
        val tuple = Tuple10(1, "a", 2.2, true, "b", "c", 42, false, "e", "f")
        tuple.e1 shouldBe 1
        tuple.e2 shouldBe "a"
        tuple.e3 shouldBe 2.2
        tuple.e4 shouldBe true
        tuple.e5 shouldBe "b"
        tuple.e6 shouldBe "c"
        tuple.e7 shouldBe 42
        tuple.e8 shouldBe false
        tuple.e9 shouldBe "e"
        tuple.e10 shouldBe "f"
    }
    "Tuple10: asList should return correctly" {
        val tuple = Tuple10(1, "a", 2.2, true, "b", "c", 42, false, "e", "f")
        tuple.asList shouldBe listOf(1, "a", 2.2, true, "b", "c", 42, false, "e", "f")
    }
})
