package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.encodeUriComponent

class UriParamBuilder private constructor() {
    companion object {
        fun of(map: Map<String, String?>): Map<String, Value> = uriParams {
            map.forEach { (k, v) -> set(k, v) }
        }

        fun uriParams(block: UriParamBuilder.() -> Unit): Map<String, Value> = UriParamBuilder().apply(block).build()
    }

    interface Value {
        val encoded: String?
    }

    data class StrValue(val str: String?) : Value {
        override val encoded = str?.encodeUriComponent()
    }

    data class NumberValue(val num: Number?) : Value {
        override val encoded = num?.toString()?.encodeUriComponent()
    }

    data class BooleanValue(val bool: Boolean?) : Value {
        override val encoded = bool?.toString()?.encodeUriComponent()
    }

    data class RawValue(val raw: String?) : Value {
        override val encoded = raw
    }

    private val values = mutableMapOf<String, Value>()

    internal fun build(): Map<String, Value> = values.toMap()

    fun set(name: String, value: Value) {
        values[name] = value
    }

    fun set(name: String, value: String?) = set(name, StrValue(value))
    fun set(name: String, value: Number?) = set(name, NumberValue(value))
    fun set(name: String, value: Boolean?) = set(name, BooleanValue(value))

    fun setRaw(name: String, value: String?) = set(name, RawValue(value))
}
