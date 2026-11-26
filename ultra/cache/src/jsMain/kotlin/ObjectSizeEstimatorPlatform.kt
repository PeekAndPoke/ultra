package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig

/** The byte length of an `ArrayBuffer`, a typed array or a `DataView`, else -1. */
private val byteLengthOf: (dynamic) -> Double = js(
    "(function(o) { return typeof ArrayBuffer !== 'undefined' && " +
        "(o instanceof ArrayBuffer || ArrayBuffer.isView(o)) ? o.byteLength : -1; })"
)

/** The size of a `Blob`, else -1. */
private val blobSizeOf: (dynamic) -> Double = js(
    "(function(o) { return typeof Blob !== 'undefined' && o instanceof Blob ? o.size : -1; })"
)

/** JS implementation that extracts fields via `Object.getOwnPropertyNames`. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    /**
     * Reads the own properties of [obj], skipping function-valued ones. A read that throws (an
     * accessor property), whatever it throws, yields an [UnreadableField]. Never returns `null`; a
     * listing of the properties that throws (a JS `Proxy`) is thrown on.
     *
     * Prototype members are not own properties, so inherited state is not seen.
     */
    // TODO(scan): reading `dyn[k]` invokes an own accessor property, so estimating can run user code
    //  - the JVM actual reads the raw field instead.
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        val dyn = obj.asDynamic()

        return ownPropertyNames(dyn).map { read(dyn, it) }.filter { it !== SkippedField }
    }

    /**
     * The own properties of [obj], each read only when the walk samples it. A function-valued one
     * reads as [SkippedField]: its reference counts, nothing more.
     */
    internal actual fun fieldsForWalk(obj: Any): WalkFields? {
        val dyn = obj.asDynamic()
        val keys = ownPropertyNames(dyn)

        return WalkFields(keys.size) { read(dyn, keys[it]) }
    }

    /**
     * An `ArrayBuffer`, a typed array or a `DataView` is charged an array header plus its byte length,
     * a `Blob` an object header plus its size. (Kotlin's primitive arrays are typed arrays too; the
     * common walk sizes them first, by the same formula.)
     */
    internal actual fun leafSizeOf(obj: Any, cfg: EstimatorConfig): Long? {
        val bytes = byteLengthOf(obj)
        if (bytes >= 0) return saturatedPlus(cfg.arrayHeader, bytes.toLong())

        val blob = blobSizeOf(obj)
        if (blob >= 0) return saturatedPlus(cfg.objectHeader, blob.toLong())

        return null
    }

    /**
     * [header] + [size] for a [size] of at least 0, at most `Long.MAX_VALUE`: a forged `byteLength` or
     * `size` of `Infinity` reads as `Long.MAX_VALUE`, and the header added to it wrapped negative.
     */
    private fun saturatedPlus(header: Long, size: Long): Long =
        if (header > 0 && size > Long.MAX_VALUE - header) Long.MAX_VALUE else header + size

    private fun ownPropertyNames(dyn: dynamic): Array<String> =
        js("Object.getOwnPropertyNames")(dyn).unsafeCast<Array<String>>()

    private fun read(dyn: dynamic, key: String): Any? {
        // catchingAll: a getter can throw a value that is no Error, which `catch (e: Throwable)` lets through
        val v: Any? = catchingAll({ dyn[key] }, { e -> UnreadableField(e.failureName()) })

        // jsTypeOf, not js("typeof v === 'function'"): the inlined form reaches for the Kotlin
        // local `v` by name, and a renamed local would make it `typeof <undeclared>`, which
        // yields "undefined" rather than throwing - i.e. the check would silently invert.
        return if (jsTypeOf(v) == "function") SkippedField else v
    }
}
