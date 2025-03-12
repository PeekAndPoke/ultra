package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.RAND
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.e2e.karangoDriver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeBetween

@Suppress("ClassName")
class `E2E-Func-Numeric-RAND-Spec` : StringSpec({

    "RAND() - direct return" {

        repeat(10) {

            val result = karangoDriver.query {
                RETURN(
                    RAND()
                )
            }

            val first = result.first()

            first.toDouble().shouldBeBetween(0.0, 1.0, 0.0)
        }
    }
})
