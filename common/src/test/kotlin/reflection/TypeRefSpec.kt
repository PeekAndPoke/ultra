package de.peekandpoke.ultra.common.reflection

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.typeOf

class TypeRefSpec : StringSpec({

    "KTypes must contain valid predefined types" {

        assertSoftly {
            TypeRef.Unit.type.classifier shouldBe Unit::class
            TypeRef.Unit.type.isMarkedNullable shouldBe false
            TypeRef.UnitNull.type.classifier shouldBe Unit::class
            TypeRef.UnitNull.type.isMarkedNullable shouldBe true

            TypeRef.Any.type.classifier shouldBe Any::class
            TypeRef.Any.type.isMarkedNullable shouldBe false
            TypeRef.AnyNull.type.classifier shouldBe Any::class
            TypeRef.AnyNull.type.isMarkedNullable shouldBe true

            TypeRef.Boolean.type.classifier shouldBe Boolean::class
            TypeRef.Boolean.type.isMarkedNullable shouldBe false
            TypeRef.BooleanNull.type.classifier shouldBe Boolean::class
            TypeRef.BooleanNull.type.isMarkedNullable shouldBe true

            TypeRef.Byte.type.classifier shouldBe Byte::class
            TypeRef.Byte.type.isMarkedNullable shouldBe false
            TypeRef.ByteNull.type.classifier shouldBe Byte::class
            TypeRef.ByteNull.type.isMarkedNullable shouldBe true

            TypeRef.Char.type.classifier shouldBe Char::class
            TypeRef.Char.type.isMarkedNullable shouldBe false
            TypeRef.CharNull.type.classifier shouldBe Char::class
            TypeRef.CharNull.type.isMarkedNullable shouldBe true

            TypeRef.Double.type.classifier shouldBe Double::class
            TypeRef.Double.type.isMarkedNullable shouldBe false
            TypeRef.DoubleNull.type.classifier shouldBe Double::class
            TypeRef.DoubleNull.type.isMarkedNullable shouldBe true

            TypeRef.Float.type.classifier shouldBe Float::class
            TypeRef.Float.type.isMarkedNullable shouldBe false
            TypeRef.FloatNull.type.classifier shouldBe Float::class
            TypeRef.FloatNull.type.isMarkedNullable shouldBe true

            TypeRef.Int.type.classifier shouldBe Int::class
            TypeRef.Int.type.isMarkedNullable shouldBe false
            TypeRef.IntNull.type.classifier shouldBe Int::class
            TypeRef.IntNull.type.isMarkedNullable shouldBe true

            TypeRef.Long.type.classifier shouldBe Long::class
            TypeRef.Long.type.isMarkedNullable shouldBe false
            TypeRef.LongNull.type.classifier shouldBe Long::class
            TypeRef.LongNull.type.isMarkedNullable shouldBe true

            TypeRef.Number.type.classifier shouldBe Number::class
            TypeRef.Number.type.isMarkedNullable shouldBe false
            TypeRef.NumberNull.type.classifier shouldBe Number::class
            TypeRef.NumberNull.type.isMarkedNullable shouldBe true

            TypeRef.Short.type.classifier shouldBe Short::class
            TypeRef.Short.type.isMarkedNullable shouldBe false
            TypeRef.ShortNull.type.classifier shouldBe Short::class
            TypeRef.ShortNull.type.isMarkedNullable shouldBe true

            TypeRef.String.type.classifier shouldBe String::class
            TypeRef.String.type.isMarkedNullable shouldBe false
            TypeRef.StringNull.type.classifier shouldBe String::class
            TypeRef.StringNull.type.isMarkedNullable shouldBe true
        }
    }

    "kType<String>() must work correctly" {

        val result = kType<String>()

        assertSoftly {
            result.type.classifier shouldBe String::class
            result.type.isMarkedNullable shouldBe false
        }
    }

    "kType<String?>() must work correctly" {

        val result = kType<String?>()

        assertSoftly {
            result.type.classifier shouldBe String::class
            result.type.isMarkedNullable shouldBe true
        }
    }

    "Create a type ref from a generic type directly must work" {

        val result = kType<List<String>>()

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "kListType<String>() must work correctly" {

        val result = kListType<String>()

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "kListType<String?> must work correctly" {

        val result = kListType<String?>()

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe true
        }
    }

    "Upping a String-type to a List-type must work" {

        val result = kType<String>().list

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "Upping a String-type to a List?-type must work" {

        val result = kType<String>().list.nullable

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe true
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "Upping a String-type to a List-type twice must work" {

        val result = kType<String>().list.list

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe List::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "Upping a String?-type to a List-type twice must work" {

        val result = kType<String?>().list.list

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe List::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.arguments[0].type!!.isMarkedNullable shouldBe true
        }
    }

    "Upping a String?-type to a nullable List-type must work" {

        val result = kType<String?>().list.nullable

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe true
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe true
        }
    }

    "Downing a List<String> type must work" {

        val result = kListType<String>().unList

        assertSoftly {
            result.type.classifier shouldBe String::class
            result.type.isMarkedNullable shouldBe false
        }
    }

    "Downing a List<String?> type must work" {

        val result = kListType<String?>().unList

        assertSoftly {
            result.type.classifier shouldBe String::class
            result.type.isMarkedNullable shouldBe true
        }
    }

    "Downing a non List-Type must throw" {

        shouldThrow<IllegalStateException> {
            @Suppress("UNCHECKED_CAST")
            (kType<String>() as TypeRef<List<String>>).unList
        }
    }

    "Creating a Map<String, Int?>-type must work" {

        val result = kMapType<String, Int?>()

        assertSoftly {
            result.type.classifier shouldBe Map::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[1].type!!.classifier shouldBe Int::class
            result.type.arguments[1].type!!.isMarkedNullable shouldBe true
        }
    }

    "Creating a Map<String?, Int>-type must work" {

        val result = kMapType<String?, Int>()

        assertSoftly {
            result.type.classifier shouldBe Map::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe true
            result.type.arguments[1].type!!.classifier shouldBe Int::class
            result.type.arguments[1].type!!.isMarkedNullable shouldBe false
        }
    }

    "Upping a Map<String, Int> type to a List type must work" {

        val result = kMapType<String, Int>().list

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe Map::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.arguments[1].type!!.classifier shouldBe Int::class
            result.type.arguments[0].type!!.arguments[1].type!!.isMarkedNullable shouldBe false
        }
    }

    "Downing a List<Map<String, Int>> type must work" {

        val result = kMapType<String, Int>().list.unList

        assertSoftly {
            @Suppress("RemoveExplicitTypeArguments")
            result shouldBe kMapType<String, Int>()

            result.type.classifier shouldBe Map::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
            result.type.arguments[1].type!!.classifier shouldBe Int::class
            result.type.arguments[1].type!!.isMarkedNullable shouldBe false
        }
    }

    "Wrapping a type with a generic class (List) must work" {

        val result = kType<String>().wrapWith<List<*>>()

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe false
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe false
        }
    }

    "Wrapping a type with a generic class (List?) must work" {

        val result = kType<String?>().wrapWith<List<*>>().nullable

        assertSoftly {
            result.type.classifier shouldBe List::class
            result.type.isMarkedNullable shouldBe true
            result.type.arguments[0].type!!.classifier shouldBe String::class
            result.type.arguments[0].type!!.isMarkedNullable shouldBe true
        }
    }

    "Creating a TypeRef from a KClass" {

        assertSoftly {
            List::class.kType() shouldBe kListType<Any>()
            @Suppress("RemoveExplicitTypeArguments")
            Int::class.kType() shouldBe kType<Int>()
        }
    }

    "Creating a TypeRef from a Java Class" {

        assertSoftly {
            List::class.java.kType() shouldBe kListType<Any>()
            @Suppress("RemoveExplicitTypeArguments")
            Int::class.java.kType() shouldBe kType<Int>()
        }
    }

    "TypeRef.reified - creation" {

        data class ExampleClass<P1, P2>(
            val p1: P1,
            val p2: P2,
        )

        val subject: ReifiedKType = kType<ExampleClass<Int, String?>>().reified


        assertSoftly {
            subject.type shouldBe typeOf<ExampleClass<Int, String?>>()
            subject.cls shouldBe ExampleClass::class

            subject.ctorParams2Types.shouldHaveSize(2)

            subject.ctorParams2Types[0].first.type.toString() shouldBe "P1"
            subject.ctorParams2Types[0].first.name shouldBe "p1"
            subject.ctorParams2Types[0].second shouldBe typeOf<Int>()

            subject.ctorParams2Types[1].first.type.toString() shouldBe "P2"
            subject.ctorParams2Types[1].first.name shouldBe "p2"
            subject.ctorParams2Types[1].second shouldBe typeOf<String?>()

            subject.properties2Types[0].first.type.toString() shouldBe "class java.lang.Object"
            subject.properties2Types[0].first.name shouldBe "p1"
            subject.properties2Types[0].second shouldBe typeOf<Int>()

            subject.properties2Types[1].first.type.toString() shouldBe "class java.lang.Object"
            subject.properties2Types[1].first.name shouldBe "p2"
            subject.properties2Types[1].second shouldBe typeOf<String?>()
        }
    }
})
