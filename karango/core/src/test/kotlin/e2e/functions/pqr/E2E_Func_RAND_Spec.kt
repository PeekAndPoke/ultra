package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeBetween
import io.peekandpoke.karango.aql.RAND
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.e2e.karangoDriver

@Suppress("ClassName")
class E2E_Func_RAND_Spec : StringSpec({

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
