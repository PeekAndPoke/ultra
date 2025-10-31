package de.peekandpoke.funktor.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.slumber

object JsonPrinter {

    private val mapper = ObjectMapper().apply {
        registerKotlinModule()

        registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .build()
        )
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())

        // serialization features
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)

        // deserialization features
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val prettyPrinter = mapper.writerWithDefaultPrettyPrinter()

    private val codec = Codec.default

    fun prettyPrint(obj: Any?): String = try {
        prettyPrinter.writeValueAsString(
            try {
                codec.slumber(obj)
            } catch (e: Throwable) {
                obj
            }
        )
    } catch (e: Throwable) {
        "Could not pretty print: $obj"
    }
}
