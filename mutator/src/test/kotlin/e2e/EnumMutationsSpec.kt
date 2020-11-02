package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class EnumMutationsSpec : StringSpec({

    "Mutating a class that has an enum property must work" {

        val source = WithSimpleEnum(enumValue = SimpleEnum.First)

        val result = source.mutate {
            enumValue = SimpleEnum.Second
        }

        source shouldNotBe result

        source.enumValue shouldBe SimpleEnum.First

        result.enumValue shouldBe SimpleEnum.Second
    }

    "Mutating a class that has an enum but not changing the value must not trigger mutation" {

        val source = WithSimpleEnum(enumValue = SimpleEnum.First)

        val result = source.mutate {
            enumValue = SimpleEnum.First
        }

        assertSoftly {
            source shouldBeSameInstanceAs result
        }
    }
})
