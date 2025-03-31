package de.peekandpoke.funktor.core.broker

import de.peekandpoke.ultra.common.reflection.TypeRef
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KType

class OutgoingConverterSpec : StringSpec({

    "Shared lookup must be used correctly" {

        class DummyConverter(val init: String) : OutgoingParamConverter {
            override fun canHandle(type: KType) = true

            override fun convert(value: Any, type: KType) = "$value $init"
        }

        val subjectOne = OutgoingConverter(listOf(DummyConverter("one")))

        val subjectTwo = OutgoingConverter(listOf(DummyConverter("two")))

        assertSoftly {

            subjectOne.convert("x", TypeRef.String) shouldBe "x one"

            subjectTwo.convert("x", TypeRef.String) shouldBe "x two"
        }
    }
})
