package io.peekandpoke.ultra.reflection

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.typeOf

open class BaseWithProp {
    open val baseProp: String = "base"
}

data class DerivedWithOwnAndInherited(
    val ownProp: Int,
    override val baseProp: String = "derived",
) : BaseWithProp()

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

    "allProperties includes inherited properties" {

        val subject = ReifiedKType(kType<DerivedWithOwnAndInherited>().type)

        val names = subject.allProperties.map { it.name }.toSet()

        assertSoftly {
            names.contains("ownProp") shouldBe true
            names.contains("baseProp") shouldBe true
        }
    }

    "allPropertiesToTypes reifies types for all properties including inherited" {

        val subject = ReifiedKType(kType<DerivedWithOwnAndInherited>().type)

        val map = subject.allPropertiesToTypes.associate { (prop, type) -> prop.name to type }

        assertSoftly {
            map["ownProp"] shouldBe typeOf<Int>()
            map["baseProp"] shouldBe typeOf<String>()
        }
    }

    "declaredProperties returns only directly declared properties" {

        val subject = ReifiedKType(kType<DerivedWithOwnAndInherited>().type)

        val names = subject.declaredProperties.map { it.name }.toSet()

        assertSoftly {
            names.contains("ownProp") shouldBe true
            names.contains("baseProp") shouldBe true
        }
    }

    "declaredPropertiesToTypes reifies types for declared properties" {

        val subject = ReifiedKType(kType<DerivedWithOwnAndInherited>().type)

        val map = subject.declaredPropertiesToTypes.associate { (prop, type) -> prop.name to type }

        assertSoftly {
            map["ownProp"] shouldBe typeOf<Int>()
            map["baseProp"] shouldBe typeOf<String>()
        }
    }

    "allProperties with generic class reifies generic type parameters" {

        data class GenericHolder<T>(val held: T, val label: String)

        val subject = ReifiedKType(kType<GenericHolder<Double>>().type)

        val map = subject.allPropertiesToTypes.associate { (prop, type) -> prop.name to type }

        assertSoftly {
            map["held"] shouldBe typeOf<Double>()
            map["label"] shouldBe typeOf<String>()
        }
    }

    "declaredProperties with generic class reifies generic type parameters" {

        data class GenericHolder<T>(val held: T, val label: String)

        val subject = ReifiedKType(kType<GenericHolder<Double>>().type)

        val map = subject.declaredPropertiesToTypes.associate { (prop, type) -> prop.name to type }

        assertSoftly {
            map["held"] shouldBe typeOf<Double>()
            map["label"] shouldBe typeOf<String>()
        }
    }

    "ctorParams2Types is empty when ctor is null" {

        // Thread has no primary constructor (only secondary constructors)
        val subject = ReifiedKType(kType<Thread>().type)

        subject.ctor shouldBe null
        subject.ctorParams2Types shouldHaveSize 0
    }
})
