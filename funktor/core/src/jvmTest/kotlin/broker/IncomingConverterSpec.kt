package de.peekandpoke.funktor.core.broker

import de.peekandpoke.funktor.core.broker.vault.IncomingPrimitiveConverter
import de.peekandpoke.ultra.common.SimpleLookup
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.security.csrf.StatelessCsrfProtection
import de.peekandpoke.ultra.security.user.UserProvider
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlin.reflect.KType

class IncomingConverterSpec : StringSpec() {

    class DummyConverter(private val init: String) : IncomingParamConverter {
        override fun canHandle(type: KType) = true
        override suspend fun convert(value: String, type: KType) = "$value $init"
    }

    class AlwaysNullConverter : IncomingParamConverter {
        override fun canHandle(type: KType): Boolean = true
        override suspend fun convert(value: String, type: KType): Any? = null
    }

    init {

        val csrf = StatelessCsrfProtection("secret", 1000, UserProvider.anonymous)

        "Shared lookup must be used correctly" {
            val lookup = IncomingConverterLookup()
            val subjectOne = IncomingConverter(csrf, lookup, SimpleLookup { listOf(DummyConverter("one")) })
            val subjectTwo = IncomingConverter(csrf, lookup, SimpleLookup { listOf(DummyConverter("two")) })

            assertSoftly {
                subjectOne.convert("x", TypeRef.String.type) shouldBe "x one"
                subjectTwo.convert("x", TypeRef.String.type) shouldBe "x two"
            }
        }

        "Converting an object from route parameters" {

            data class MyClass(val name: String)

            val subject = IncomingConverter(
                csrf,
                IncomingConverterLookup(),
                SimpleLookup { listOf(IncomingPrimitiveConverter()) }
            )

            subject.convert(
                parametersOf("name", "funktor"),
                parametersOf("name", "funktor-query-param"),
                kType<MyClass>()
            ) shouldBe MyClass("funktor")
        }

        "Converting an object from query parameters" {

            data class MyClass(val name: String)

            val subject = IncomingConverter(
                csrf,
                IncomingConverterLookup(),
                SimpleLookup { listOf(IncomingPrimitiveConverter()) }
            )

            subject.convert(
                parametersOf("other", "funktor"),
                parametersOf("name", "funktor-query-param"),
                kType<MyClass>()
            ) shouldBe MyClass("funktor-query-param")
        }

        "Nullable route-parameters can be converted to null" {

            data class MyClass(val name: String?)

            class AlwaysNullConverter : IncomingParamConverter {
                override fun canHandle(type: KType): Boolean = true
                override suspend fun convert(value: String, type: KType): Any? = null
            }

            val subject = IncomingConverter(
                csrf,
                IncomingConverterLookup(),
                SimpleLookup { listOf(AlwaysNullConverter()) }
            )

            val result = subject.convert(
                parametersOf("name", "..."),
                parametersOf(),
                kType<MyClass>()
            )

            result shouldBe MyClass(name = null)
        }

        "Must throw when Non-nullable route-parameter was converted to null" {

            data class MyClass(val name: String)

            val subject = IncomingConverter(
                csrf,
                IncomingConverterLookup(),
                SimpleLookup { listOf(AlwaysNullConverter()) }
            )

            shouldThrow<NotFoundException> {
                subject.convert(
                    parametersOf("name", "..."),
                    parametersOf(),
                    kType<MyClass>()
                )
            }
        }
    }
}
