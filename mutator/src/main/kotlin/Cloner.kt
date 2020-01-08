package de.peekandpoke.ultra.mutator

import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

object Cloner {

    fun <T : Any> cloneDataClass(subject: T): T {

        val cls = subject::class

        val primaryCtr = cls.primaryConstructor

        return when {
            // Data class
            cls.isData -> {

                // TODO: cache the first part, where the information is gathered

                // every data class has a primary constructor
                val ctor = cls.primaryConstructor!!

                // get all fields of the data class in the order as they appear in the primary constructor
                val fields = ctor.parameters.map { param ->

                    val field = cls.memberProperties.first { it.name == param.name }.javaField!!

                    field.isAccessible = true
                    field.get(subject)
                }

                // invoke the copy() method with the values
                val copy = cls.declaredFunctions.first { it.name == "copy" }

                @Suppress("UNCHECKED_CAST")
                copy.call(subject, *fields.toTypedArray()) as T
            }

            // No arg primary constructor
            primaryCtr != null && primaryCtr.parameters.isEmpty() -> cls.createInstance()

            // TODO: more specific exception
            else -> throw Exception(
                "Cannot clone type '${subject::class}'. It is not a data class and has no no-arg constructor!"
            )
        }
    }
}
