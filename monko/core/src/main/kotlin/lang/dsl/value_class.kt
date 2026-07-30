package io.peekandpoke.monko.lang.dsl

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Reduces a SCALAR-backed `@JvmInline value class` to its underlying scalar so the MongoDB driver can
 * encode it as a filter/update value — matching the plain scalar that slumber STORES for it.
 *
 * MongoDB filter/update values are NOT routed through slumber (unlike stored documents, which are):
 * the DSL operators hand the value straight to `com.mongodb.client.model.Filters` / `Updates`, whose
 * codec registry has no codec for an arbitrary value class and throws `CodecConfigurationException`.
 * Without this reduction, `entity.realm eq RealmId("b2b")` type-checks but fails at query execution.
 *
 * SCALAR-BACKED ONLY. This unwraps by REFLECTION (reads the backing property), not through slumber, so
 * it only reproduces the stored shape when the backing is a driver-encodable scalar (`String` /
 * `Number` / `Boolean`) — which every id we wrap is. A value class over a specially-serialized type
 * (a date/time, an enum, a `Ref`, a nested object) would serialize differently in the stored document
 * than a reflected raw value, which would make a filter SILENTLY match nothing; so a non-scalar
 * backing is rejected LOUDLY here at build time rather than producing wrong query results. (True parity
 * for such types would require routing the value through slumber, which the pure DSL has no codec for.)
 *
 * The backing value is read from the PRIMARY-CONSTRUCTOR property (a value class may also declare extra
 * computed properties, so `memberProperties.first()` is not safe). Recurses for a value class over a
 * value class. Kotlin stdlib value classes (`Duration`, `UInt`, ...) are left untouched — they need
 * dedicated codecs and are excluded exactly as slumber's `isUserValueClass` excludes them.
 *
 * A non-value-class value (`String`, `Int`, a data class, a collection, `null`) is returned unchanged —
 * so direct object-equality filters keep working.
 */
internal fun unwrapValueClass(value: Any?): Any? {
    // Fast path: the overwhelmingly common filter/update values are plain scalars — skip reflection.
    if (value == null || value is String || value is Number || value is Boolean) {
        return value
    }

    var current: Any? = value
    var unwrapped = false

    while (current != null) {
        val cls = current::class

        if (!cls.isValue || cls.qualifiedName?.startsWith("kotlin.") == true) {
            break
        }

        val backingName = cls.primaryConstructor?.parameters?.firstOrNull()?.name ?: break
        val prop = cls.memberProperties.firstOrNull { it.name == backingName } ?: break
        prop.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        current = (prop as KProperty1<Any, *>).get(current)
        unwrapped = true
    }

    // A value class only round-trips to its stored form when its backing is a driver-encodable scalar.
    // Reject a non-scalar backing loudly (rather than silently querying a divergent shape). This only
    // fires for a value class we actually reduced — a plain object compared directly never gets here.
    require(!unwrapped || current == null || current is String || current is Number || current is Boolean) {
        "Monko filter/update values support only scalar-backed @JvmInline value classes " +
            "(String/Number/Boolean). ${value::class.simpleName} reduces to ${current!!::class.simpleName}; " +
            "use a scalar backing or route the value through slumber."
    }

    return current
}
