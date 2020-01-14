package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.assertSoftly
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

class EnumMutationsSpec : StringSpec({

    "Mutating a class that has an enum property must work" {

        val source = WithSimpleEnum(enumValue = SimpleEnum.First)

        val result = source.mutate {
            enumValue = SimpleEnum.Second
        }

        assertSoftly {
            source shouldNotBe result

            source.enumValue shouldBe SimpleEnum.First

            result.enumValue shouldBe SimpleEnum.Second
        }
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
