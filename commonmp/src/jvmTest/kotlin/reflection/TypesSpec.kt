package de.peekandpoke.ultra.reflection

import de.peekandpoke.ultra.common.reflection.isPrimitive
import de.peekandpoke.ultra.common.reflection.primitiveTypes
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PagedSpec : FreeSpec() {

    init {
        "primitiveTypes" {
            primitiveTypes shouldBe setOf(
                String::class,
                Byte::class,
                Short::class,
                Int::class,
                Long::class,
                Float::class,
                Double::class,
                Boolean::class,
            )
        }

        "KClass.isPrimitive" {
            primitiveTypes.forEach {
                withClue("isPrimitive($it) must be true") {
                    it.isPrimitive() shouldBe true
                }
            }

            withClue("isPrimitive(Any) must be false") {
                Any::class.isPrimitive() shouldBe false
            }
        }
    }
}
