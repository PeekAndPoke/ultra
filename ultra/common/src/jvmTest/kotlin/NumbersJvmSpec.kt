package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class NumbersJvmSpec : StringSpec({

    "toFixed uses a dot regardless of the default locale" {
        val original = Locale.getDefault()

        try {
            // a German default renders 1,50 unless the formatter pins its locale, which would
            // make a server disagree with its own JS client over the same number
            Locale.setDefault(Locale.GERMANY)

            1.5.toFixed(2) shouldBe "1.50"
            1234.5.toFixed(1) shouldBe "1234.5"
            0.0.toFixed(2) shouldBe "0.00"
        } finally {
            Locale.setDefault(original)
        }
    }
})
