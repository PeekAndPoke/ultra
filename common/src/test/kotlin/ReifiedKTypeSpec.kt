package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class ReifiedKTypeSpec : StringSpec({

    "Reifying a simple class" {

        data class Simple(val value: Int)

        val subject = ReifiedKType(kType<Simple>().type)

        assertSoftly {
            subject.ctorParams2Types[0].first.name shouldBe "value"
            subject.ctorParams2Types[0].second shouldBe TypeRef.Int.type
        }
    }

    "Reifying a class with one generic parameter" {

        data class Generic<T>(val value: T)

        val subject1 =
            ReifiedKType(kType<Generic<Int>>().type)
        val subject2 =
            ReifiedKType(kType<Generic<String>>().type)

        assertSoftly {
            subject1.ctorParams2Types[0].first.name shouldBe "value"
            subject1.ctorParams2Types[0].second shouldBe TypeRef.Int.type

            subject2.ctorParams2Types[0].first.name shouldBe "value"
            subject2.ctorParams2Types[0].second shouldBe TypeRef.String.type
        }
    }

    "Reifying a class with one generic parameter passed through" {

        data class Generic<T>(val value: T)

        val subject1 =
            ReifiedKType(kType<Generic<List<Int>>>().type)

        assertSoftly {
            subject1.ctorParams2Types[0].first.name shouldBe "value"
            subject1.ctorParams2Types[0].second shouldBe TypeRef.Int.list.type
        }
    }

    "Reifying a class with one generic parameter passing another generic class in" {

        data class Inner<X>(val inner: X)

        data class Generic<T>(val value: Inner<T>)

        val subject1 =
            ReifiedKType(kType<Generic<Int>>().type)

        assertSoftly {
            subject1.ctorParams2Types[0].first.name shouldBe "value"
            subject1.ctorParams2Types[0].second shouldBe kType<Inner<Int>>().type
        }
    }
})
