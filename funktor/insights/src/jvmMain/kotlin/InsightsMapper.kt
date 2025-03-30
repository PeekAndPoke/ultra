package de.peekandpoke.ktorfx.insights

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

class InsightsMapper : ObjectMapper() {

    init {
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
}
