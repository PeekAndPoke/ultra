package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.kontainer.InvalidClassProvided
import io.peekandpoke.ultra.kontainer.ServiceProducer

class JavaStyleClass {
    @Suppress("unused")
    constructor(value: Int) {
        // secondary constructor only, no primary
    }
}

class ServiceProducerSpec : StringSpec({

    "forClass with interface must throw InvalidClassProvided" {
        val error = shouldThrow<InvalidClassProvided> {
            ServiceProducer.forClass(Runnable::class)
        }

        error.message shouldContain "interface or abstract class"
    }

    "forClass with abstract class must throw InvalidClassProvided" {
        val error = shouldThrow<InvalidClassProvided> {
            ServiceProducer.forClass(AbstractCollection::class)
        }

        error.message shouldContain "interface or abstract class"
    }

    "forClass with class without primary constructor must throw InvalidClassProvided" {
        val error = shouldThrow<InvalidClassProvided> {
            ServiceProducer.forClass(JavaStyleClass::class)
        }

        error.message shouldContain "no primary constructor"
    }
})
