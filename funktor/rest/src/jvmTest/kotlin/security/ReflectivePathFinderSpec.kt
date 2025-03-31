package de.peekandpoke.funktor.rest.security

import de.peekandpoke.funktor.rest.security.ReflectivePathFinder.Companion.findAnnotatedElementPaths
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.full.starProjectedType

class ReflectivePathFinderSpec : StringSpec() {

    @SensitiveData
    data class TestClass(
        val inner1: Inner1,
        val inner2: Inner2,
    ) {
        data class Inner1(
            @SensitiveData
            val param1: String,
            val param2: String,
        )

        @SensitiveData
        data class Inner2(
            val param: Int,
        )
    }


    init {
        "Simple Object hierarchy" {

            val result = kType<TestClass>().findAnnotatedElementPaths {
                it.hasAnnotation<SensitiveData>()
            }

            result shouldBe listOf(
                ReflectivePathFinder.FoundItem(
                    path = listOf(),
                    type = TestClass::class.starProjectedType,
                ),
                ReflectivePathFinder.FoundItem(
                    path = listOf("inner1", "param1"),
                    type = String::class.starProjectedType,
                ),
                ReflectivePathFinder.FoundItem(
                    path = listOf("inner2"),
                    type = TestClass.Inner2::class.starProjectedType,
                )
            )
        }

        "Wrapped with multi level generic class" {

            val result = kType<ApiResponse<Paged<TestClass>>>().findAnnotatedElementPaths {
                it.hasAnnotation<SensitiveData>()
            }

            result shouldBe listOf(
                ReflectivePathFinder.FoundItem(
                    path = listOf("data", "items", "*"),
                    type = TestClass::class.starProjectedType,
                ),
                ReflectivePathFinder.FoundItem(
                    path = listOf("data", "items", "*", "inner1", "param1"),
                    type = String::class.starProjectedType,
                ),
                ReflectivePathFinder.FoundItem(
                    path = listOf("data", "items", "*", "inner2"),
                    type = TestClass.Inner2::class.starProjectedType,
                )
            )
        }
    }
}
