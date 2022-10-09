package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.reflect.typeOf

class CodecSpec : StringSpec({

    "Caching data class awakers must work for simple data classes" {

        data class DataClass(val i: Int)
        data class DataClass2(val i: Int)

        val codec = Codec.default

        codec.getAwaker(kType<DataClass>()) shouldBeSameInstanceAs
                codec.getAwaker(kType<DataClass>())

        codec.getAwaker(typeOf<DataClass>()) shouldBeSameInstanceAs
                codec.getAwaker(typeOf<DataClass>())

        codec.getAwaker(kType<DataClass>()) shouldBeSameInstanceAs
                codec.getAwaker(typeOf<DataClass>())

        codec.getAwaker(kType<DataClass>()) shouldNotBeSameInstanceAs
                codec.getAwaker(kType<DataClass2>())
    }

    "Caching data class slumberers must work for simple data classes" {

        data class DataClass(val i: Int)
        data class DataClass2(val i: Int)

        val codec = Codec.default

        codec.getSlumberer(kType<DataClass>()) shouldBeSameInstanceAs
                codec.getSlumberer(kType<DataClass>())

        codec.getSlumberer(typeOf<DataClass>()) shouldBeSameInstanceAs
                codec.getSlumberer(typeOf<DataClass>())

        codec.getSlumberer(kType<DataClass>()) shouldBeSameInstanceAs
                codec.getSlumberer(typeOf<DataClass>())

        codec.getSlumberer(kType<DataClass>()) shouldNotBeSameInstanceAs
                codec.getSlumberer(kType<DataClass2>())
    }

    "Caching generic data class awakers must work for simple data classes" {

        data class DataClass<T>(val i: T)
        data class DataClass2<T>(val i: T)

        val codec = Codec.default

        codec.getAwaker(kType<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getAwaker(kType<DataClass<Int>>())

        codec.getAwaker(typeOf<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getAwaker(typeOf<DataClass<Int>>())

        codec.getAwaker(kType<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getAwaker(typeOf<DataClass<Int>>())

        codec.getAwaker(kType<DataClass<Int>>()) shouldNotBeSameInstanceAs
                codec.getAwaker(kType<DataClass<String>>())

        codec.getAwaker(kType<DataClass<Int>>()) shouldNotBeSameInstanceAs
                codec.getAwaker(kType<DataClass2<Int>>())
    }

    "Caching generic data class slumberers must work for simple data classes" {

        data class DataClass<T>(val i: T)
        data class DataClass2<T>(val i: T)

        val codec = Codec.default

        codec.getSlumberer(kType<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getSlumberer(kType<DataClass<Int>>())

        codec.getSlumberer(typeOf<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getSlumberer(typeOf<DataClass<Int>>())

        codec.getSlumberer(kType<DataClass<Int>>()) shouldBeSameInstanceAs
                codec.getSlumberer(typeOf<DataClass<Int>>())

        codec.getSlumberer(kType<DataClass<Int>>()) shouldNotBeSameInstanceAs
                codec.getSlumberer(kType<DataClass<String>>())

        codec.getSlumberer(kType<DataClass<Int>>()) shouldNotBeSameInstanceAs
                codec.getSlumberer(kType<DataClass2<Int>>())
    }
})
